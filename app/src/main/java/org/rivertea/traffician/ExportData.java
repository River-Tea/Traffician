package org.rivertea.traffician;

public class ExportData {
    private StringBuilder exportData;

    public ExportData() {
    }

    public ExportData(StringBuilder exportData) {
        this.exportData = exportData;
    }

    public StringBuilder getExportData() {
        return exportData;
    }

    public void setExportData(StringBuilder exportData) {
        this.exportData = exportData;
    }
}
