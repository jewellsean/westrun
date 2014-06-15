package westrun;

import java.io.File;

import westrun.exprepo.ExperimentsRepository;

import binc.Command;
import briefj.BriefIO;

import static binc.Command.call;



public class Sync
{
  public static void main(String [] args)
  {
    sync();
  }
  
  public static void sync()
  {
    ExperimentsRepository repo = ExperimentsRepository.fromWorkingDirectoryParents();
    System.out.println("Starting sync. Note: if files were deleted, this may recreate them.");
    
    call(rsync
      .ranIn(repo.root())
      .withArgs(
        "--exclude-from=" + new File(repo.configDir(), IGNORE_FILE) + " " +
        "-u " +
        "-r " + 
        repo.root().getAbsolutePath() + "/ " +  
        repo.getSSHString())
      .saveOutputTo(new File(repo.configDir(), "synclog1")));
    
    call(rsync
        .ranIn(repo.root())
        .withArgs(
          "--exclude-from=" + new File(repo.configDir(), IGNORE_FILE) + " " +
          "-u " +
          "-r " + 
          repo.getSSHString() + "/ " +  
          repo.root().getAbsolutePath())
        .saveOutputTo(new File(repo.configDir(), "synclog2")));
    
    System.out.println("Sync complete. See .westrun/synclog{1,2} for details");
  }
  
  public static final Command rsync = Command.byName("rsync").throwOnNonZeroReturnCode();

  public static void createExcludeList()
  {
    ExperimentsRepository repo = ExperimentsRepository.fromWorkingDirectoryParents();
    String exclude = 
      "/" + ExperimentsRepository.CONFIG_DIR + "\n" +
      "/" + NewExperiment.DRAFTS_FOLDER_NAME + "\n" +
      "/" + Launch.CODE_TO_TRANSFER + "\n" + // this is taken care by a special sync in Launch for efficiency
      "/" + Launch.TRANSFERRED_CODE + "\n" + 
      "/" + Launch.RAN_TEMPLATE_DIR_NAME;
    BriefIO.write(new File(repo.configDir(), IGNORE_FILE), exclude);
  }
  
  public static final String IGNORE_FILE = "syncignore";
}
