package org.jdownloader.extensions.webinterfaceng.actions;

import java.util.List;

import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.jdownloader.extensions.webinterfaceng.data.FilePackageDO;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.opensymphony.xwork2.ActionSupport;

@ParentPackage("default")
public class ListDownloads extends ActionSupport {

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;

    // Your result List
    private List<FilePackageDO> gridModel;

    // get how many rows we want to have into the grid - rowNum attribute in the
    // grid
    private Integer             rows             = 15;

    // Get the requested page. By default grid sets this to 1.
    private Integer             page             = 1;

    // sorting order - asc or desc
    private String              sord;

    // get index row - i.e. user click to sort.
    private String              sidx;

    // Search Field
    private String              searchField;

    // The Search String
    private String              searchString;

    // he Search Operation
    // ['eq','ne','lt','le','gt','ge','bw','bn','in','ni','ew','en','cn','nc']
    private String              searchOper;

    // Your Total Pages
    private Integer             total            = 0;

    // All Record
    private Integer             records          = 0;

    @Actions({ @Action(value = "ListDownloads", results = { @Result(name = "success", type = "json") }) })
    public String execute() {

        int to = (rows * page);
        int from = to - rows;

        List<FilePackage> packages = Lists.newArrayList(JDUtilities.getController().getPackages());
        Ordering<FilePackage> ordering = Ordering.natural().onResultOf(new Function<FilePackage, Comparable<?>>() {
            public Comparable<?> apply(FilePackage arg0) {
                if ("percent".equals(sidx)) {
                    return new Double(arg0.getPercent());
                } else if ("hoster".equals(sidx)) {
                    return arg0.getHoster();
                } else if ("name".equals(sidx)) {
                    return arg0.getName();
                } else {
                    return new Integer(arg0.getListOrderID());
                }
            }
        });

        if ("desc".equals(sord)) {
            ordering = ordering.reverse();
        }

        packages = ordering.sortedCopy(packages);

        // Count Rows (select count(*) from custumer)
        records = packages.size();
        to = Math.min(to, records);
        to = Math.max(to, 0);

        packages = packages.subList(from, to);

        // Your logic to search and select the required data.
        gridModel = Lists.newArrayList(Lists.transform(packages, new Function<FilePackage, FilePackageDO>() {
            public FilePackageDO apply(FilePackage arg0) {
                return new FilePackageDO(arg0);
            }
        }));

        // calculate the total pages for the query
        total = (int) Math.ceil((double) records / (double) rows);

        return SUCCESS;
    }

    public String getJSON() {
        return execute();
    }

    public List<FilePackageDO> getGridModel() {
        return gridModel;
    }

    public void setGridModel(List<FilePackageDO> gridModel) {
        this.gridModel = gridModel;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getSord() {
        return sord;
    }

    public void setSord(String sord) {
        this.sord = sord;
    }

    public String getSidx() {
        return sidx;
    }

    public void setSidx(String sidx) {
        this.sidx = sidx;
    }

    public String getSearchField() {
        return searchField;
    }

    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchOper() {
        return searchOper;
    }

    public void setSearchOper(String searchOper) {
        this.searchOper = searchOper;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getRecords() {
        return records;
    }

    public void setRecords(Integer records) {
        this.records = records;
    }

    // Getters and Setters for Attributes
}