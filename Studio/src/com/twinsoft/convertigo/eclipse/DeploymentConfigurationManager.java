package com.twinsoft.convertigo.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DeploymentConfigurationManager {

    private static final String DEPLOYMENT_CONFIGURATION_DB = Engine.USER_WORKSPACE_PATH + "/studio/deployment_configurations.db";
    private static final String OLD_TRIAL_REGISTRATION_DB = Engine.USER_WORKSPACE_PATH + "/studio/trial_deploy.ser";
    
    private Map<String, DeploymentConfiguration> deploymentConfigurationsReadOnly;
	private Map<String, DeploymentConfiguration> deploymentConfigurations;
	private Map<String, String> defaultDeploymentConfigurations;

	private SecretKey secretKey;
	private DESKeySpec desKeySpec;
	private SecretKeyFactory secretKeyFactory;
	
	public DeploymentConfigurationManager() {
		deploymentConfigurationsReadOnly = new HashMap<String, DeploymentConfiguration>();
		deploymentConfigurations = new HashMap<String, DeploymentConfiguration>();
		defaultDeploymentConfigurations = new HashMap<String, String>();

		try {
			byte key[] = DESKeySpec.class.getCanonicalName().toString().getBytes();
	        desKeySpec = new DESKeySpec(key);
	        secretKeyFactory = SecretKeyFactory.getInstance("DES");
	        secretKey = secretKeyFactory.generateSecret(desKeySpec);
		}
	    catch (Exception e){
	    	ConvertigoPlugin.logException(e, "Unable to generate the secret key");
	    }
	}
	
	public void doMigration() throws EngineException, IOException, ClassNotFoundException {
		List<String> projectsNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false);		 
		ObjectInputStream objectInputStream = null;		
		DeploymentInformation deploymentInformation = null;
		
		load();
		
		ConvertigoPlugin.logDebug("Projects deployment configurations migration starting...");

		for (String projectName: projectsNames) {			
			String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/_private/deploy.ser";			
			File deploySer = new File(filePath);
			deploymentInformation = null;

			if (deploySer.exists()) {				
		       	try {
		       		objectInputStream = new ObjectInputStream(new FileInputStream(filePath));
		       		deploymentInformation = (DeploymentInformation) objectInputStream.readObject();
		        } catch (FileNotFoundException e) {
		        	ConvertigoPlugin.logError("Unable to migrate the deployment configurations for project " + projectName+ ": " 
							+ e.getMessage());
		        } catch (IOException e) {
		        	ConvertigoPlugin.logError("Unable to migrate the deployment configurations for project " + projectName+ ": " 
		        			+ e.getMessage());
				} catch (ClassNotFoundException e) {
					ConvertigoPlugin.logError("Unable to migrate the deployment configurations for project " + projectName+ ": " 
							+ e.getMessage());
				} catch (Exception e) {
					ConvertigoPlugin.logError("Unable to migrate the deployment configurations for project " + projectName+ ": " 
							+ e.getMessage());
				}
		        finally {
		        	if (objectInputStream != null) objectInputStream.close();
		        }
			}
			
			if (deploymentInformation != null) {				
				for (DeploymentConfiguration deploymentConfiguration: deploymentInformation.deploymentConfigurations.values()) {
					add(deploymentConfiguration);
				}
			}

			try {
				objectInputStream = new ObjectInputStream(new FileInputStream(OLD_TRIAL_REGISTRATION_DB));
				DeploymentConfiguration deploymentConfiguration = (DeploymentConfiguration) objectInputStream.readObject();
				add(deploymentConfiguration);
			}
			catch(Exception e) {
			  // Ignore
			}
			
			save();
		}		
		ConvertigoPlugin.logDebug("Projects deployment configurations migration finished");
	}
	
	public DeploymentConfiguration getDefault(String projectName) {		
		String key = defaultDeploymentConfigurations.get(projectName);
		return key == null ? null : get(key);
	}
	
	public void setDefault(String projectName, String deploymentConfigurationName) {		
		defaultDeploymentConfigurations.put(projectName, deploymentConfigurationName);
	}
	
	public Set<String> getAllDeploymentConfigurationNames() {
		Set<String> all = new LinkedHashSet<String>(deploymentConfigurationsReadOnly.keySet());
		all.addAll(deploymentConfigurations.keySet());
		return all;
	}
	
	public DeploymentConfiguration get(String name) {
		DeploymentConfiguration deploymentConfiguration = deploymentConfigurations.get(name);
		return deploymentConfiguration != null ? deploymentConfiguration : deploymentConfigurationsReadOnly.get(name);
	}
	
	public void add(DeploymentConfigurationReadOnly deploymentConfigurationReadOnly) {
		deploymentConfigurationsReadOnly.put(deploymentConfigurationReadOnly.getServer(), deploymentConfigurationReadOnly);
	}
	
	public void add(DeploymentConfiguration deploymentConfiguration) {
		deploymentConfigurations.put(deploymentConfiguration.getServer(), deploymentConfiguration);
	}
	
	public void remove(DeploymentConfiguration deploymentConfiguration) throws IOException {
		deploymentConfigurations.remove(deploymentConfiguration.getServer());
	}

	public void save() throws IOException {
		Object[] objectsToSerialize = {deploymentConfigurations, defaultDeploymentConfigurations};
		
		ObjectOutputStream objectOutputStream = null;
		CipherOutputStream encrypt = null;
		try {	
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			encrypt = new CipherOutputStream(new FileOutputStream(DEPLOYMENT_CONFIGURATION_DB), cipher);
			objectOutputStream = new ObjectOutputStream(encrypt);
            objectOutputStream.writeObject(objectsToSerialize);
        }
        catch(IOException e) {
        	ConvertigoPlugin.logException(e, "Unable to save the deployment configurations.");
        } catch (NoSuchAlgorithmException e) {
        	ConvertigoPlugin.logException(e, "Unable to find the encryption algorithm: " + e.getMessage());	
		} catch (NoSuchPaddingException e) {
			ConvertigoPlugin.logException(e, "Unable to find the padding mechanism from your environment: " + e.getMessage());
		} catch (InvalidKeyException e) {
			ConvertigoPlugin.logException(e, "Invalid encryption key: " + e.getMessage());
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "An error occured saving deployment configurations: " + e.getMessage());
		}
        finally {
        	if (objectOutputStream != null) {
        		objectOutputStream.close();
        	}
        	ConvertigoPlugin.logDebug("Projects deployment configurations saved");
        }
	}
	
	public void load() throws IOException, ClassNotFoundException {
		ObjectInputStream objectInputStream = null;
		CipherInputStream decode = null;
       	try {
       		Cipher cipher = Cipher.getInstance("DES");
       		cipher.init(Cipher.DECRYPT_MODE, secretKey);
       		decode = new CipherInputStream(new FileInputStream(DEPLOYMENT_CONFIGURATION_DB), cipher); 
       		objectInputStream = new ObjectInputStream(decode);
       		Object [] deserializedObjects = GenericUtils.cast(objectInputStream.readObject());       		
       		deploymentConfigurations = GenericUtils.cast(deserializedObjects[0]);
       		defaultDeploymentConfigurations = GenericUtils.cast(deserializedObjects[1]);
        } catch (NoSuchAlgorithmException e) {
        	// Use the default empty database
        	ConvertigoPlugin.logException(e, "Unable to find the encryption algorithm: " + e.getMessage());	
    	} catch (NoSuchPaddingException e) {
    		// Use the default empty database
    		ConvertigoPlugin.logException(e, "Unable to find the padding mechanism from your environment: " + e.getMessage());
    	} catch (InvalidKeyException e) {
    		// Use the default empty database
    		ConvertigoPlugin.logException(e, "Invalid encryption key: " + e.getMessage());
    	} catch (FileNotFoundException e) {
			// Use the default empty database
    		add(new DeploymentConfiguration(
				"localhost:28080/convertigo",
				"admin", "admin",
				false, false, false)
    		);
		} catch (Exception e) {
			ConvertigoPlugin.logError("Unable to load the deployment configurations: " + e.getMessage() + "\n. A new empty list will be created.");
        	// Use the default empty database
		}
        
        finally {
        	if (objectInputStream != null) {
        		objectInputStream.close();
        	}
        	ConvertigoPlugin.logDebug("Projects deployment configurations loaded");
        }
	}
}
