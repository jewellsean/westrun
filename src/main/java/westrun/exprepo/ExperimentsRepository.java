package westrun.exprepo;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;


import binc.BinCallUtils;
import briefj.BriefFiles;
import briefj.BriefIO;
import briefj.BriefStrings;
import briefj.opt.Option;
import briefj.run.OptionsUtils;
import briefj.unix.RemoteUtils;


/**
 * A repository of experiments. Contains the information read
 * from a config file. The main purpose of this class is to 
 * keep centralized info on the subdirectory structure used by
 * westrun.
 * 
 * @author Alexandre Bouchard (alexandre.bouchard@gmail.com)
 *
 */
public class ExperimentsRepository
{
  public final String sshRemoteHost;
  
  /*
   * These are absolute paths.
   */
  public final File localExpRepoRoot;
  public final File remoteExpRepoRoot;
  public final File localCodeRepoRoot;
  
  public final ExperimentsRepoConfig configuration;
  
  private ExperimentsRepository(ExperimentsRepoConfig config, File localExpRepoRoot)
  {
    this.localExpRepoRoot = localExpRepoRoot;
    this.sshRemoteHost = config.sshRemoteHost;
    this.remoteExpRepoRoot = new File(config.remoteDirectory);
    this.localCodeRepoRoot = new File(config.codeRepository);
    this.configuration = config;
  }

  public static ExperimentsRepository fromWorkingDirParents()
  {
    File configFile = new File(BriefFiles.findFileInParents(ExpRepoPath.CONFIG.getName()), ExpRepoPath.MAIN_CONFIG_FILE.getName()); ///CONFIG_DIR), MAIN_CONFIG_FILE);
    ExperimentsRepoConfig config = ExperimentsRepoConfig.fromJSON(configFile);
    File localExpRepoRoot = configFile.getParentFile().getParentFile();
    return new ExperimentsRepository(config, localExpRepoRoot);
  }
  
  public static ExperimentsRepository fromCommandLineArguments(String [] args)
  {
    // gather info
    ExperimentsRepoConfig config = new ExperimentsRepoConfig();
    OptionsUtils.parseOptions(args, config);
    File localExpRepoRoot = BriefFiles.currentDirectory();
    
    // resolve remote home
    resolveRemoteHome(config, localExpRepoRoot);
    
    return new ExperimentsRepository(config, localExpRepoRoot);
  }
  
//  public static final String CONFIG_DIR = ".westrun";
//  public static final String MAIN_CONFIG_FILE = "config.json";
  
  public String getSSHString()
  {
    return "" + sshRemoteHost + ":" + remoteExpRepoRoot;
  }
  
  public File resolveLocal(ExpRepoPath path)
  {
    return path.buildFile(localExpRepoRoot);
  }
  
  public File resolveRemote(ExpRepoPath path)
  {
    return path.buildFile(remoteExpRepoRoot);
  }
  

  
  private static void resolveRemoteHome(ExperimentsRepoConfig config, File localExpRepoRoot)
  {
    if (StringUtils.isEmpty(config.remoteDirectory))
    {
      // find remote home
      String home = RemoteUtils.remoteBash(config.sshRemoteHost, "echo ~");
      config.remoteDirectory = home + "/" + localExpRepoRoot.getName();
    }
  }
}
