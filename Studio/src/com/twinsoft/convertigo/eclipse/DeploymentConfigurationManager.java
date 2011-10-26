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
    
	private Map<String, DeploymentConfiguration> deploymentConfigurations;
	
	public Map<String, DeploymentConfiguration> defaultDeploymentConfigurations;
	
	private SecretKey secretKey;
	private DESKeySpec desKeySpec;
	private SecretKeyFactory secretKeyFactory;
	
	public DeploymentConfigurationManager() {
		deploymentConfigurations = new HashMap<String, DeploymentConfiguration>();
		defaultDeploymentConfigurations = new HashMap<String, DeploymentConfiguration>();

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
		List<String> projectsNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();		 
		ObjectInputStream objectInputStream = null;		
		DeploymentInformation deploymentInformation = null;
		
		load();

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
	}
	
	public DeploymentConfiguration getDefault(String projectName) {		
		for (String defaultProjectName: defaultDeploymentConfigurations.keySet()) {			
			if (projectName.equals(defaultProjectName)) {
				return defaultDeploymentConfigurations.get(defaultProjectName);
			}
		}
		return null;
	}
	
	public void setDefault(String projectName, DeploymentConfiguration deploymentConfiguration) {		
		defaultDeploymentConfigurations.put(projectName, deploymentConfiguration);
	}
	
	public Set<String> getAllDeploymentConfigurationNames() {
		return deploymentConfigurations.keySet();	
	}
	
	public DeploymentConfiguration get(String name) {
		return deploymentConfigurations.get(name);
	}
	
	public void add(DeploymentConfiguration deploymentConfiguration) {
		deploymentConfigurations.put(deploymentConfiguration.getServer(), deploymentConfiguration);
	}
	
	public void remove(DeploymentConfiguration deploymentConfiguration) throws IOException {
		deploymentConfigurations.remove(deploymentConfiguration.getServer());
	}

	public void save() throws IOException {
		ObjectOutputStream objectOutputStream = null;
		CipherOutputStream encrypt = null;
		try {	
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			encrypt = new CipherOutputStream(new FileOutputStream(DEPLOYMENT_CONFIGURATION_DB), cipher);
			objectOutputStream = new ObjectOutputStream(encrypt);
            objectOutputStream.writeObject(deploymentConfigurations);
        }
        catch(IOException e) {
        	ConvertigoPlugin.logException(e, "Unable to save the deployment configurations.");
        } catch (NoSuchAlgorithmException e) {
        	ConvertigoPlugin.logException(e, "Unable to find the encryption algorithm: " + e.getMessage());	
		} catch (NoSuchPaddingException e) {
			ConvertigoPlugin.logException(e, "Unable to find the padding mechanism from your environment: " + e.getMessage());
		} catch (InvalidKeyException e) {
			ConvertigoPlugin.logException(e, "Invalid encryption key: " + e.getMessage());
		}
        finally {
        	if (objectOutputStream != null) objectOutputStream.close();
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
       		deploymentConfigurations = GenericUtils.cast(objectInputStream.readObject());
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
        } catch (IOException e) {
        	ConvertigoPlugin.logError("Unable to load the deployment configurations: " + e.getMessage() + "\n. A new empty list will be created.");
        	// Use the default empty database
		}
        finally {
        	if (objectInputStream != null) objectInputStream.close();
        }
	}
}
