package westrun;

import static binc.Command.call;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import westrun.code.SelfBuiltRepository;
import westrun.exprepo.ExperimentsRepository;
import westrun.template.PrepareExperiments;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;

import briefj.BriefIO;
import briefj.BriefStrings;
import briefj.opt.InputFile;
import briefj.opt.Option;
import briefj.repo.GitRepository;
import briefj.run.Mains;
import briefj.run.Results;
import briefj.unix.RemoteUtils;



public class Launch implements Runnable
{
  @InputFile(copy = true)
  @Option(required = true)
  public File templateFile;
  
  @Option(condReq = "test=false")
  public String description;
  
  @Option
  public boolean test = false;
  
  private ExperimentsRepository repo;

  @Override
  public void run()
  {
    repo = ExperimentsRepository.fromWorkingDirectoryParents();
    
    String codeRepo = "";
    // clone code repo
    if (!StringUtils.isEmpty(repo.codeRepository))
    {
      // clone
      File repository = cloneRepository();
      
      // build
      if (SelfBuiltRepository.loadSpecification(repository) != null)
        SelfBuiltRepository.build(repository);
      
      File local1  = new File(repo.root(), CODE_TO_TRANSFER);
      
      // transfer code delta
      call(Sync.rsync
          .ranIn(repo.root())
          .withArgs(
            "--delete-after " +
            "-u " +
            "-r " + 
            local1.getAbsolutePath() + "/ " +  
            repo.getSSHString() + "/" + CODE_TO_TRANSFER)
          .saveOutputTo(new File(repo.configDir(), "codesynclog")));
      
      // copy to unique location
      File local2 = new File(new File(repo.root(), TRANSFERRED_CODE), updatedCodeName());
      
      try { FileUtils.copyDirectory(local1, local2); }
      catch (Exception e) { throw new RuntimeException(e); }
      
      String remote1 = repo.remoteDirectory + "/" + CODE_TO_TRANSFER;
      String remote2 = repo.remoteDirectory + "/" + TRANSFERRED_CODE + "/" + updatedCodeName();
      
      RemoteUtils.remoteBash(repo.sshRemoteHost, Arrays.asList(
          "mkdir " + repo.remoteDirectory + "/" + TRANSFERRED_CODE + ">/dev/null 2>&1",
          "cp -r " +  remote1 + " " + remote2));
      
      codeRepo = remote2;
    }
    
    // prepare scripts
    List<File> launchScripts = PrepareExperiments.prepare(templateFile, repo.root().getName(), test, codeRepo, new File(repo.codeRepository).getName());
    
    // sync up
    Sync.sync();
    
    // run the commands (Later: collect the id?)
    System.out.println("Launch result=" + launch(launchScripts) + "|");
    
    // move template to previous-template folder
    if (!test)
    {
      File previousTemplateDir = new File(repo.root(), RAN_TEMPLATE_DIR_NAME);
      previousTemplateDir.mkdir();
      File destination = new File(previousTemplateDir, updatedCodeName());
      templateFile.renameTo(destination);
      System.out.println("Executed template file moved to " + destination.getAbsolutePath());
    }
  }
  
  public String updatedCodeName()
  {
    return Results.getResultFolder().getName().replace(".exec", "");
  }
  
  public static final String RAN_TEMPLATE_DIR_NAME = "previous-templates";

  public static final String CODE_TO_TRANSFER = ".codeToTransfer";
  public static final String TRANSFERRED_CODE = ".transferredCode";
  
  public static void main(String [] args) throws InvalidRemoteException, TransportException, GitAPIException
  {
    Mains.instrumentedRun(args, new Launch());
  }

  private File cloneRepository()
  {
    File localCodeRepository = new File(repo.codeRepository);
    GitRepository gitRepo = GitRepository.fromLocal(localCodeRepository);
    String commitId = gitRepo.getCommitIdentifier(); 
    BriefIO.write(Results.getFileInResultFolder("codeCommitIdentifier"), commitId);
    
    List<File> dirtyFile = gitRepo.dirtyFiles();
    if (!dirtyFile.isEmpty())
      throw new RuntimeException("There are dirty files in the repository: " + Joiner.on("\n").join(dirtyFile));
    
    File destination = new File(repo.root(), CODE_TO_TRANSFER); //Results.getFolderInResultFolder("code");
    try { FileUtils.deleteDirectory(destination); } 
    catch (IOException e) { throw new RuntimeException(e); }
    
    try 
    {
      Git.cloneRepository()
        .setURI(localCodeRepository.getAbsolutePath())
        .setDirectory(destination)
        .call();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    
    return destination;
  }

  private String launch(List<File> launchScripts)
  {
    String remoteLaunchCommand = test ? "bash" : "qsub";
    
    List<String> commands = Lists.newArrayList();
    commands.add("cd " + repo.remoteDirectory);
    for (File launchScript : launchScripts)
      commands.add(remoteLaunchCommand + " " + launchScript);
    System.out.println(commands);
    return RemoteUtils.remoteBash(repo.sshRemoteHost, commands);
  }


}
