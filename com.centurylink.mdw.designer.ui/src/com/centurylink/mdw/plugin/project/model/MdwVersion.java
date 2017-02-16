/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

public class MdwVersion {
    private String version;

    public MdwVersion(String mdwVersion) {
        this.version = mdwVersion;
    }

    public boolean checkRequiredVersion(String requiredVersion) throws NumberFormatException {
        String[] parts = requiredVersion.split("\\.");
        if (parts.length == 1)
            return checkRequiredVersion(Integer.parseInt(parts[0]));
        else if (parts.length == 2)
            return checkRequiredVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        else if (parts.length == 3)
            return checkRequiredVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        else
            return false;
    }

    public boolean checkRequiredVersion(int major) {
        return checkRequiredVersion(major, 0);
    }

    public boolean checkRequiredVersion(int major, int minor) {
        return checkRequiredVersion(major, minor, 0);
    }

    public boolean checkRequiredVersion(int major, int minor, int build) {
        if (version == null || version.length() == 0)
            return false;

        int mdwMajor = getMajorVersion();
        if (mdwMajor > major)
            return true;
        if (mdwMajor < major)
            return false;

        // major versions are equal
        int mdwMinor = getMinorVersion();
        if (mdwMinor > minor)
            return true;
        if (mdwMinor < minor)
            return false;

        // major and minor are equal
        return getBuildId() >= build;
    }

    public int getMajorVersion() {
        int idxFirstDot = version.indexOf('.');
        return Integer.parseInt(version.substring(0, idxFirstDot));
    }

    public int getMinorVersion() {
        int idxFirstDot = version.indexOf('.');
        int idxSecondDot = version.indexOf('.', idxFirstDot + 1);
        return idxSecondDot == -1 ? Integer.parseInt(version.substring(idxFirstDot + 1))
                : Integer.parseInt(version.substring(idxFirstDot + 1, idxSecondDot));
    }

    public int getBuildId() {
        int idxFirstDot = version.indexOf('.');
        int idxSecondDot = version.indexOf('.', idxFirstDot + 1);
        int lastDash = version.lastIndexOf('-');
        if (idxSecondDot == -1)
            return 0;
        else if (lastDash == -1)
            return Integer.parseInt(version.substring(idxSecondDot + 1));
        else // trim -SNAPSHOT
            return Integer.parseInt(version.substring(idxSecondDot + 1, lastDash));
    }

}
