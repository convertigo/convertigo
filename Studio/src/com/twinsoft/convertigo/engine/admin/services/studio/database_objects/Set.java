package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IEnableAble;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.EnumUtils;

@ServiceDefinition(
	name = "Set",
	roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG },
	parameters = {},
	returnValue = "the state of saving properties"
)
public class Set extends XmlService {
	
	private Object getPropertyValue(DatabaseObject object, String propertyName, String propertyValue)
			throws TransformerException {
		return object.compileProperty(propertyName, propertyValue);
	}

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		Element response = document.createElement("response");
		
		try {			
			String objectQName = request.getParameter("qname");
			String value = request.getParameter("value");
			String property = request.getParameter("property");

			DatabaseObject object = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(objectQName);

			// Check if we try to update project name
			if (object instanceof Project && "name".equals(property)) {
				Project project = (Project) object;
				
				String objectNewName = getPropertyValue(object, property, value).toString();
				
				Engine.theApp.databaseObjectsManager.renameProject(project, objectNewName);
			}

			BeanInfo bi = CachedIntrospector.getBeanInfo(object.getClass());

			PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
			
			boolean propertyFound = false;
			for (int i = 0; !propertyFound && i < propertyDescriptors.length; ++i) {
				String propertyName = propertyDescriptors[i].getName();
				
				// Find the property we want to change
				if (propertyName.equals(property)) {
					Method setter = propertyDescriptors[i].getWriteMethod();
					
					Class<?> propertyTypeClass = propertyDescriptors[i].getReadMethod().getReturnType();
					if (propertyTypeClass.isPrimitive()) {
						propertyTypeClass = ClassUtils.primitiveToWrapper(propertyTypeClass);
					}
					
					try{
						String propertyValue = getPropertyValue(object, propertyName, value).toString(); 
	
						Object oPropertyValue = createObject(propertyTypeClass, propertyValue);
		
						if (object.isCipheredProperty(propertyName)) {
							
							Method getter = propertyDescriptors[i].getReadMethod();
							String initialValue = (String) getter.invoke(object, (Object[]) null);
							
							if (oPropertyValue.equals(initialValue) || 
									DatabaseObject.encryptPropertyValue(initialValue).equals(oPropertyValue)) {
								oPropertyValue = initialValue;
							}else{
								object.hasChanged = true;
							}
						}
						
						if (oPropertyValue != null) {
							Object args[] = { oPropertyValue };
							setter.invoke(object, args);
						}
						
					} catch(IllegalArgumentException e){}
					
					propertyFound = true;
				}
			}
			
			// Invalid given property parameter
			if (!propertyFound) {
				throw new IllegalArgumentException("Property '" + property
						+ "' not found for object '" + object.getQName() + "'");
			}
			
			Engine.theApp.databaseObjectsManager.exportProject(object.getProject());
			response.setAttribute("state", "success");
			response.setAttribute("message", "Project have been successfully updated!");
			
			Element elt = com.twinsoft.convertigo.engine.admin.services.database_objects.Get.getProperties(object, document, object.getQName());
			elt.setAttribute("classname", object.getClass().getName());
			elt.setAttribute("name", object.toString());
			elt.setAttribute("hasChanged", Boolean.toString(object.hasChanged));
			elt.setAttribute("isEnabled", object instanceof IEnableAble ? Boolean.toString(((IEnableAble) object).isEnabled()) : "null");

			root.appendChild(elt);
		} catch(Exception e){
			Engine.logAdmin.error("Error during saving the properties!\n"+e.getMessage());
			response.setAttribute("state", "error");
			response.setAttribute("message", "Error during saving the properties!");
			Element stackTrace = document.createElement("stackTrace");
			stackTrace.setTextContent(e.getMessage());
			root.appendChild(stackTrace);
		}

		root.appendChild(response);
	}
	
	private Object createObject(Class<?> propertyClass, String value) throws ServiceException {
		Object oPropertyValue = null;

		if (Number.class.isAssignableFrom(propertyClass) ||
				Boolean.class.isAssignableFrom(propertyClass) ||
				String.class.isAssignableFrom(propertyClass)) {
			try {
				oPropertyValue = propertyClass.getConstructor(String.class).newInstance(value);	
			} catch (Exception e) {
				throw new ServiceException("Error when create the object:\n"+e.getMessage());
			}
		} else if (Enum.class.isAssignableFrom(propertyClass)) {
			oPropertyValue = EnumUtils.valueOf(propertyClass, value);
		}
		
		return oPropertyValue;
	}
}