
# lib_UserManager
Here is how to set up the project for the different login providers:

- **Google**
    1. Log in to https://console.developers.google.com
    2. Create a **NEW PROJECT**.
    3. Go to **Credentials > Create credentials > OAuth client ID**
    4. Select **Web application**
    5. Choose a name for your *OAuth client ID*
    6. Add **Authorised JavaScript origins** like:
        - http://localhost:18080 for testing with your local Studio
        - https://c8ocloud.convertigo.net
    7. Add following **Authorised redirect URIs**:
        - https://c8ocloud.convertigo.net/convertigo/projects/libOAuth/getTokenGoogle.html
        
- **LinkedIn**
    1. Log in to https://linkedin.com/developers
    2. Go to **My Apps > Create app**
    3. Fill in the form to create your application.
    4. Go to **Auth > Application credentials**
        - Copy **Client ID** and **Client Secret** keys to Convertigo Symbols
    5. Go to **Auth > OAuth 2.0 settings** and set Redirect URLs:
        - https://c8ocloud.convertigo.net/convertigo/projects/libOAuth/getTokenlinkedIn.html

- **Azure**
    1. Log in to https://portal.azure.com
    2. Go to **All Services > Azure Active Directory > App registrations**
    3. Click **New registration**.
        - Choose a name.
        - In **Supported account types**, select Single or Multi tenant.
        - In **Redirect URI (optional)**, select **Public client/native**
        - Type in https://c8ocloud.convertigo.net/convertigo/projects/libOAuth/getToken.html
        - Click **Register**
    4. Click your project name:
        - Go to **Authentication > Advanced settings** and check **Access tokens** and **ID tokens**
        - Copy **Application (client) ID** and **Directory (tenant) ID** keys to Convertigo Symbols

The project also contains a sharedComponent component named "**LoginComponent**" you can use in your own projects. This requires the lib_OAuth library project.
