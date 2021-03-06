/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.CommaParameterSplitter;

public abstract class Setup implements Operation {

    protected static List<String> defaultBasePackages = new ArrayList<>();
    static {
        defaultBasePackages.add("com.centurylink.mdw.base");
        defaultBasePackages.add("com.centurylink.mdw.db");
        defaultBasePackages.add("com.centurylink.mdw.testing");
    }

    Setup() {
    }

    Setup(File projectDir) {
        this.projectDir = projectDir;
    }

    protected File projectDir;
    public File getProjectDir() {
        return projectDir == null ? new File(".") : projectDir;
    }

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    public String getMdwVersion() { return mdwVersion; }
    public void setMdwVersion(String version) {
        this.mdwVersion = version;
        if (Props.Gradle.MDW_VERSION != null)
            Props.Gradle.MDW_VERSION.specified = true;
    }
    public String findMdwVersion() throws IOException {
        if (getMdwVersion() == null) {
            URL url = new URL(getReleasesUrl() + "/com/centurylink/mdw/mdw-templates/");
            Crawl crawl = new Crawl(url, isSnapshots());
            crawl.run();
            if (crawl.getReleases().size() == 0)
                throw new IOException("Unable to locate MDW releases: " + url);
            mdwVersion = crawl.getReleases().get(crawl.getReleases().size() - 1);
        }
        return mdwVersion;
    }

    @Parameter(names="--debug", description="Display CLI debug information")
    private boolean debug;
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    @Parameter(names="--snapshots", description="Whether to include snapshot builds")
    private boolean snapshots;
    public boolean isSnapshots() { return snapshots; }
    public void setSnapshots(boolean snapshots) { this.snapshots = snapshots; }

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = "http://repo.maven.apache.org/maven2";
    public String getDiscoveryUrl() { return discoveryUrl; }

    public void setDiscoveryUrl(String url) {
        this.discoveryUrl = url;
        if (Props.DISCOVERY_URL != null)
            Props.DISCOVERY_URL.specified = true;
    }

    @Parameter(names="--releases-url", description="MDW releases Maven repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";
    public String getReleasesUrl() {
        return releasesUrl.endsWith("/") ? releasesUrl.substring(0, releasesUrl.length() - 1) : releasesUrl;
    }
    public void setReleasesUrl(String url) {
        this.releasesUrl = url;
        Props.Gradle.MAVEN_REPO_URL.specified = true;
    }

    @Parameter(names="--services-url", description="MDW service base URL")
    private String servicesUrl = "http://localhost:8080/mdw/services";
    public String getServicesUrl() {
        return servicesUrl.endsWith("/") ? servicesUrl.substring(0, servicesUrl.length() - 1) : servicesUrl;
    }
    public void setServicesUrl(String url) {
        this.servicesUrl = url;
        Props.SERVICES_URL.specified = true;
    }

    @Parameter(names="--config-loc", description="Config location (when outside project dir)")
    private String configLoc;
    public String getConfigLoc() { return configLoc; }
    public void setConfigLoc(String configLoc) { this.configLoc = configLoc; }

    @Parameter(names="--asset-loc", description="Asset location")
    private String assetLoc = "assets";
    public String getAssetLoc() { return assetLoc; }
    public void setAssetLoc(String assetLoc) {
        this.assetLoc = assetLoc;
        if (Props.ASSET_LOC != null)
            Props.ASSET_LOC.specified = true;
    }

    @Parameter(names="--base-asset-packages", description="MDW Base Asset Packages (comma-separated)",
            splitter=CommaParameterSplitter.class)
    private List<String> baseAssetPackages;
    public List<String> getBaseAssetPackages() { return baseAssetPackages; }
    public void setBaseAssetPackages(List<String> packages) { this.baseAssetPackages = packages; }

    // Git Parameters used by Import (otherwise only for templates)
    @Parameter(names="--git-remote-url", description="Git repository URL")
    private String gitRemoteUrl = "https://github.com/CenturyLinkCloud/mdw-demo.git";
    public String getGitRemoteUrl() { return gitRemoteUrl; }
    public void setGitRemoteUrl(String url) {
        this.gitRemoteUrl = url;
        Props.Git.REMOTE_URL.specified = true;
    }

    @Parameter(names="--git-branch", description="Git branch")
    private String gitBranch = "master";
    public String getGitBranch() { return gitBranch; }
    public void setGitBranch(String branch) {
        this.gitBranch = branch;
        Props.Git.BRANCH.specified = true;
    }

    @Parameter(names="--git-user", description="Git user")
    private String gitUser = "anonymous";
    public String getGitUser() { return gitUser; }
    public void setGitUser(String user) {
        this.gitUser = user;
        Props.Git.USER.specified = true;
    }

    @Parameter(names="--git-password", description="Git password")
    private String gitPassword;
    String getGitPassword() { return this.gitPassword; }
    public void setGitPassword(String password) {
        this.gitPassword = password;
        Props.Git.PASSWORD.specified = true;
    }

    @Parameter(names="--database-url", description="JDBC URL (without credentials)")
    private String databaseUrl = "jdbc:mariadb://localhost:3308/mdw";
    public String getDatabaseUrl() { return databaseUrl; }
    public void setDatabaseUrl(String url) {
        this.databaseUrl = url;
        Props.Db.URL.specified = true;
    }

    @Parameter(names="--database-user", description="DB User")
    private String databaseUser = "mdw";
    public String getDatabaseUser() { return databaseUser; }
    public void setDatabaseUser(String user) {
        this.databaseUser = user;
        Props.Db.USER.specified = true;
    }

    @Parameter(names="--database-password", description="DB Password")
    private String databasePassword = "mdw";
    public String getDatabasePassword() { return databasePassword; }
    public void setDatabasePassword(String password) {
        this.databasePassword = password;
        Props.Db.PASSWORD.specified = true;
    }

    private String databaseDriver = "org.mariadb.jdbc.Driver";
    public String getDatabaseDriver() {
        String driver = DbInfo.getDatabaseDriver(getDatabaseUrl());
        return driver == null ? databaseDriver : driver;
    }
    public void setDatabaseDriver(String driver) { this.databaseDriver = driver; }

    /**
     * Checks for any existing packages.  If none present, adds the defaults.
     */
    protected void initBaseAssetPackages() throws IOException {
        baseAssetPackages = new ArrayList<>();
        addBasePackages(getAssetRoot(), getAssetRoot());
        if (baseAssetPackages.isEmpty())
            baseAssetPackages = defaultBasePackages;
    }

    private void addBasePackages(File assetDir, File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create directory: " + dir.getAbsolutePath());
        if (!dir.isDirectory())
            throw new IOException("Expected directory: " + dir.getAbsolutePath());

        if (new File(dir + "/.mdw/package.json").isFile()) {
            String pkg = getAssetPath(dir).replace('/', '.');
            if (pkg.startsWith("com.centurylink.mdw."))
                baseAssetPackages.add(pkg);
        }
        for (File child : dir.listFiles()) {
            if (child.isDirectory())
                addBasePackages(assetDir, child);
        }
    }

    static final Pattern SUBST_PATTERN = Pattern.compile("\\{\\{(.*?)}}");

    protected void subst(File dir) throws IOException {
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                subst(child);
            }
            else {
                String contents = new String(Files.readAllBytes(Paths.get(child.getPath())));
                StringBuilder newContents = new StringBuilder(contents.length());
                int index = 0;
                Matcher matcher = SUBST_PATTERN.matcher(contents);
                while (matcher.find()) {
                    String match = matcher.group();
                    newContents.append(contents.substring(index, matcher.start()));
                    Object value = getValue(match.substring(2, match.length() - 2));
                    if (value == null)
                        value = match;
                    newContents.append(value == null ? "" : value);
                    index = matcher.end();
                }
                newContents.append(contents.substring(index));
                if (newContents.toString().equals(contents)) {
                    System.out.println("   " + child);
                }
                else {
                    Files.write(Paths.get(child.getPath()), newContents.toString().getBytes());
                    System.out.println("   " + child + " *");
                }
            }
        }
    }

    public Object getValue(String name) {
        name = toCamel(name);
        Field field = null;
        try {
            field = Init.class.getDeclaredField(name);
        }
        catch (NoSuchFieldException ex) {
            try {
                field = Setup.class.getDeclaredField(name);
            }
            catch (NoSuchFieldException ex2) {
                // no subst
            }
        }
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(this);
            }
            catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    public String toCamel(String hyphenated) {
        StringBuilder nameBuilder = new StringBuilder(hyphenated.length());
        boolean capNextChar = false;
        for (char c : hyphenated.toCharArray()) {
            if (c == '-') {
                capNextChar = true;
                continue;
            }
            if (capNextChar) {
                nameBuilder.append(Character.toUpperCase(c));
            }
            else {
                nameBuilder.append(c);
            }
            capNextChar = false;
        }
        return nameBuilder.toString();
    }

    public File getConfigRoot() {
        if (configLoc != null)
            return new File(configLoc);
        return new File(getProjectDir() + "/config");
    }

    /**
     * Returns 'to' file or dir path relative to 'from' dir.
     * Result always uses forward slashes and has no trailing slash.
     */
    public String getRelativePath(File from, File to) {
        Path fromPath = Paths.get(from.getPath()).normalize().toAbsolutePath();
        Path toPath = Paths.get(to.getPath()).normalize().toAbsolutePath();
        return fromPath.relativize(toPath).toString().replace('\\', '/');
    }

    public String getAssetPath(File file) throws IOException {
        return getRelativePath(getAssetRoot(), file);
    }

    public String getPackageName(String assetPath) {
        int lastSlash = assetPath.lastIndexOf('/');
        return lastSlash > 0 ? assetPath.substring(0, lastSlash).replace('/', '.') : null;
    }
    public String getAssetName(String assetPath) {
        int lastSlash = assetPath.lastIndexOf('/');
        return lastSlash < assetPath.length() ? assetPath.substring(lastSlash + 1) : assetPath;
    }

    public File getAssetRoot() throws IOException {
        String assetLoc;
        if (Props.ASSET_LOC != null)
            assetLoc = new Props(this).get(Props.ASSET_LOC, false);
        else
            assetLoc = getAssetLoc();
        File assetRoot = new File(assetLoc);
        if (assetRoot.isAbsolute())
            return assetRoot;
        else
            return new File(getProjectDir() + "/" + assetLoc);
    }

    public File getGitRoot() throws IOException {
        Props props = new Props(this);
        String gitLocalPath = props.get("mdw.git.local.path");
        if (gitLocalPath != null)
            return new File(gitLocalPath);
        return getProjectDir();
    }

    public String getGitPath(File file) throws IOException {
        return getRelativePath(getGitRoot(), file);
    }

    public boolean gitExists() throws IOException {
        return new File(getGitRoot() + "/.git").isDirectory();
    }

    public String getMdwConfig() {
        String mdwConfig = null;
        File mdwYaml = new File(getConfigRoot() + "/mdw.yaml");
        if (mdwYaml.isFile()) {
            mdwConfig = mdwYaml.getName();
        }
        else {
            File mdwProps = new File(getConfigRoot() + "/mdw.properties");
            if (mdwProps.isFile()) {
                mdwConfig = mdwProps.getName();
            }
        }
        return mdwConfig;
    }

    public File getMdwHome() throws IOException {
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null)
            mdwHome = System.getProperty("mdw.home");
        if (mdwHome == null)
            throw new IOException("Missing environment variable: MDW_HOME");
        File mdwDir = new File(mdwHome);
        if (!mdwDir.isDirectory())
            throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
        return mdwDir;
    }

    public String getTemplatesUrl() throws IOException {
        String mdwVer = getMdwVersion();
        if (mdwVer == null)
            mdwVer = new Props(this).get(Props.Gradle.MDW_VERSION);
        String templates = "mdw-templates-" + mdwVer + ".zip";
        String templatesUrl;
        String templatesLoc = System.getProperty("mdw.templates.url");
        if (templatesLoc != null) {
            // allows loading templates from a directory for testing
            templatesUrl = templatesLoc;
        }
        else {
            if (isSnapshots()) {
                templatesUrl = "https://oss.sonatype.org/service/local/artifact/maven/redirect"
                        + "?r=snapshots&g=com.centurylink.mdw&a=mdw-templates&v=LATEST&p=zip";
            }
            else {
                templatesUrl = getReleasesUrl() + "/com/centurylink/mdw/mdw-templates/"
                        + mdwVer + "/" + templates;
            }
        }
        return templatesUrl;
    }

    /**
     * Override for extended debug info (always calling super.debug()).
     */
    public boolean validate() throws IOException {
        // check config
        if (needsConfig() && getMdwConfig() == null) {
            throw new IOException("Error: Missing config (mdw.yaml or mdw.properties)");
        }

        String projPath = Paths.get(getProjectDir().getPath()).toAbsolutePath().normalize().toString();
        String assetPath = Paths.get(getAssetRoot().getPath()).toAbsolutePath().normalize().toString();

        // normalize windows drive letter
        if (projPath.charAt(1) == ':')
            projPath = projPath.substring(0, 1).toLowerCase() + projPath.substring(1);
        if (assetPath.charAt(1) == ':')
            assetPath = assetPath.substring(0, 1).toLowerCase() + assetPath.substring(1);

        if (!assetPath.startsWith(projPath)) {
            System.err.println("Error: Asset root (" + assetPath + ") is not a subdirectory of Project (" + projPath + ")");
            return false;
        }

        return true;
    }

    protected boolean needsConfig() {
        return true;
    }

    /**
     * Override for extended debug info (always calling super.debug()).
     */
    public void debug() throws IOException {
        status();
        System.out.println("CLI Props:");
        Props props = new Props(this);
        for (Prop prop : Props.ALL_PROPS) {
            System.out.println("  " + prop);
            System.out.println("    = " + props.get(prop, false));
        }
    }

    public void status() throws IOException {
        System.out.println("Project Dir:\n " + getProjectDir().getAbsolutePath());
        System.out.println("Config Root:\n " + getConfigRoot());
        System.out.println("Asset Root:\n  " + getAssetRoot());
        System.out.println("Git Root:\n  " + getGitRoot());
    }

    /**
     * Creates a package if it doesn't exist.
     */
    public void mkPackage(String name) throws IOException {
        File metaDir = new File(getAssetLoc() + "/" + name.replace('.', '/') + "/.mdw");
        if (!metaDir.exists() && !metaDir.mkdirs())
            throw new IOException("Cannot create directory: " + metaDir.getAbsolutePath());
        File pkgFile = new File(metaDir + "/package.json");
        if (!pkgFile.exists()) {
            JSONObject pkgJson = new JSONObject();
            pkgJson.put("name", name);
            pkgJson.put("version", "1.0.01");
            pkgJson.put("schemaVersion", "6.0");
            Files.write(Paths.get(pkgFile.getPath()), pkgJson.toString(2).getBytes());
        }
    }

    protected File getTemplateDir() throws IOException {
        String mdwVer = new Props(this).get(Props.Gradle.MDW_VERSION);
        return new File(getMdwHome() + "/lib/" + mdwVer);
    }

    /**
     * Downloads asset templates for codegen, etc.
     */
    protected void downloadTemplates(ProgressMonitor... monitors) throws IOException {
        File templateDir = getTemplateDir();
        if (!templateDir.exists()) {
            if (!templateDir.mkdirs())
                throw new IOException("Unable to create directory: " + templateDir.getAbsolutePath());
            String templatesUrl = getTemplatesUrl();
            System.out.println("Retrieving templates: " + templatesUrl);
            File tempZip = Files.createTempFile("mdw-templates-", ".zip").toFile();
            new Download(new URL(templatesUrl), tempZip).run(monitors);
            File tempDir = Files.createTempDirectory("mdw-templates-").toFile();
            new Unzip(tempZip, tempDir, false).run();
            File codegenDir = new File(templateDir + "/codegen");
            new Copy(new File(tempDir + "/codegen"), codegenDir, true).run();
            File assetsDir = new File(templateDir + "/assets");
            new Copy(new File(tempDir + "/assets"), assetsDir, true).run();
        }
    }

    protected Map<String,String> parseMap(String parameter) {
        Map<String,String> map = new HashMap<>();
        for (String nameVal : parameter.split(",")) {
            int eq = nameVal.indexOf("=");
            map.put(nameVal.substring(0, eq), nameVal.substring(eq + 1));
        }
        return map;
    }

}
