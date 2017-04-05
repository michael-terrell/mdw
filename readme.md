![xml formatter](docs/help/images/mdw_sm.png)

## MDW

### Developer Setup
1. Prerequisites
 - Eclipse Neon for JavaEE Developers:                              
   [http://www.eclipse.org/downloads]([http://www.eclipse.org/downloads)
   
 - Required Plugins:
     - MDW Designer:                                         
       [Installing and Upgrading the MDW Plugin for Eclipse](docs/tutorial/InstallAndUpgradeMDWPluginforEclipse)
     - Buildship Plugin:                            
       [http://download.eclipse.org/buildship/updates/e46/releases/2.x](http://download.eclipse.org/buildship/updates/e46/releases/2.x)
       
 - Recommended Plugins:
     - Groovy:                                   
       [http://dist.springsource.org/snapshot/GRECLIPSE/e4.6](http://dist.springsource.org/snapshot/GRECLIPSE/e4.6)
     - Yaml:                                             
       [http://dadacoalition.org/yedit](http://dadacoalition.org/yedit)
       
 - Tomcat 8:                                  
     - [https://tomcat.apache.org](https://tomcat.apache.org)
       
 - Chrome and Postman:                                            
     - [https://www.google.com/chrome](https://www.google.com/chrome)
     - [https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop)
	 
2. Get the Source Code
 - Command-line Git:  
   `git clone https://github.com/CenturyLinkCloud/MDW.git mdw`
 - in Eclipse:  
   Import the project into your Eclipse workspace:  
   File > Import > General > Existing Projects into Workspace
   
3. Set up npm and Bower (One-time step)
 - Install NodeJS:                                                                     
   [https://nodejs.org/en/download/current](https://nodejs.org/en/download/current)
 - Open a command prompt in the mdw-hub project directory
 ```
   run `npm install`
   run `npm install -g grunt-cli`
   run `npm install -g bower`
 ```
4. Build the Project
 - Window > Show View > Other > Gradle  > Gradle Tasks
   Select the mdw project and Select Show all Tasks. Expand Other folder in mdw project and double-click the "buildAll" task

5. Use [Embedded DB](/mdw-workflow/assets/com/centurylink/mdw/db/readme.md) or set up an external MySQL database as described in [this readme](/mdw/database/mysql/readme.txt)
   
6. Deploy on Tomcat in Eclipse
 - Edit mdw/config/mdw.properties to suit your environment.
 - Edit mdw/config/access.yaml to set devUser to yourself.
 - In Eclipse Servers view, right-click and select New > Server  
   **Important**: Select the Apache > Tomcat 8.0 (MDW) runtime
   and make you have a jdk 1.8 installed and added it to your class path.
 - Select the mdw-hub module in the Add/Remove wizard page
 - Double-click on the server and set the startup timeout to something large (like 3600s)
 - Under MDW Server Options set the following Java Options (appropriate for your workspace):
 
```	
   -Dmdw.runtime.env=dev  
   -Dmdw.config.location=c:/workspaces/MDW/mdw/config  
   -Djavax.net.ssl.trustStore=c:/workspaces/MDW/mdw/deploy/certs/CenturyLinkQCA.jks  
   -Djava.net.preferIPv4Stack=true  
   -Dmdw.runtime.env=dev
   -Xms512m -Xmx1024m -XX:MaxPermSize=256m
```

1. Run
 - Right-click on the server and select Debug to start it up (this should automatically publish mdw-hub)
 - Check MDWHub access:                                                
   http://localhost:8080/mdw
   
2. Code Format
 - Java, Groovy, Javascript and JSON:
   The Eclipse code formatters are version-controlled in .settings/org.eclipse.jdt.core.prefs, so as long as you're up-to-date with Git you should automatically have the correct settings. If you want to use them for another project, you can download and import them from these formatter files:   
     - Java/Groovy: https://github.com/CenturyLinkCloud/MDW/docs/MDWCodeFormatter.xml   
     - Javascript/JSON: https://github.com/CenturyLinkCloud/MDW/docs/mdw-javascript-formatter.xml   
     - Please note that we use **spaces instead of tabs** for indenting all source code.
 - XML, HTML and YAML:  
   These have to be configured manually in Eclipse.  For all formats we use **spaces instead of tabs**.
   The following screenshots illustrate how to set these:  
     - XML:                                                    
      [xml formatter](docs/help/images/xmlformat.png)
     - HTML:                                                           
      [html formatter](docs/help/images/htmlformat.png)
     - YAML:                                           
      [yaml formatter](docs/help/images/yamlformat.png)

3. Designer Development
 - The designer codebase is contained in the following projects:
     	- com.centurylink.mdw.designer (gradle parent)
     	- com.centurylink.mdw.designer.core
     	- com.centurylink.mdw.designer.feature
     	- com.centurylink.mdw.designer.rcp
     	- com.centurylink.mdw.designer.ui
     
 - Build Designer
     - These projects use the MDW 5.5 versions of mdw-common as mdw-schemas as dependencies, so to you need these MDW 5.5 projects locally or to have their jars available through a repository.
   - Assuming you've got the MDW 5.5 source code locally, in com.centurylink.mdw.designer/gradle.properties, set mdwVersion and mdwOutputDir to point to this location.
   - (One time) Run an MDW 5.5 build in its workspace and then in com.centurylink.mdw.designer.core, run the gradle task getMdwCommon to copy in the 5.5 dependencies.
   - (Subsequently) When changes are made to common code in MDW 5.5 and an Eclipse build is performed in that workspace, running devGetMdwCommon will incrementally copy these.
   - Now an Eclipse build of the Designer projects should show no errors.
 - Debug Designer
   - To run through Eclipse, right-click on project com.centurylink.mdw.designer.ui and select Debug As > Eclipse Application.
