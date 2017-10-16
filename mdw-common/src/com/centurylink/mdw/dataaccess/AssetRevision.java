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
package com.centurylink.mdw.dataaccess;

import java.util.Date;

public class AssetRevision {

    private int version;
    private String modUser;
    private Date modDate;
    private String comment;

    public int getVersion() { return version; }
    public void setVersion(int ver) { this.version = ver; }

    public AssetRevision() {
    }

    public AssetRevision(String ver) {
        this.version = parseVersion(ver);
    }

    public String getFormattedVersion() {
        return "v" + getVersionString();
    }

    public String getVersionString() {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;

    }

    public String getModUser() { return modUser; }
    public void setModUser(String user) { this.modUser = user; }

    public Date getModDate() { return modDate; }
    public void setModDate(Date date) { this.modDate = date; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public static int parseVersion(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        if (versionString.startsWith("v"))
            versionString = versionString.substring(1);
        int dot = versionString.indexOf('.');
        int major, minor;
        if (dot > 0) {
            major = Integer.parseInt(versionString.substring(0, dot));
            minor = Integer.parseInt(versionString.substring(dot + 1));
        }
        else {
            major = 0;
            minor = Integer.parseInt(versionString);
        }
        return major * 1000 + minor;
    }

}
