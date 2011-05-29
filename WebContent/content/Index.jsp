<?xml version="1.0" encoding="utf-8"?>
<jsp:root xmlns="http://www.w3.org/1999/xhtml"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:s="/struts-tags"
	xmlns:sj="/struts-jquery-tags"
	xmlns:sjg="/struts-jquery-grid-tags"
	version="2.0">
	
	<jsp:output doctype-root-element="html"
		doctype-public="-//W3C//DTD XHTML 1.1//EN"
		doctype-system="http://www.w3c.org/TR/xhtml11/DTD/xhtml11.dtd" />
	<jsp:directive.page contentType="text/html; charset=utf-8"
		language="java" />

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<sj:head jqueryui="true"/>
<title>JDownloader NG</title>
</head>
<body>
    <s:url id="listDownloadsUrl" namespace="/" action="ListDownloads"/>
    <sjg:grid
        id="gridtable"
        caption="Packages"
        dataType="json"
        href="%{listDownloadsUrl}"
        pager="true"
        gridModel="gridModel"
        rowList="10,15,20"
        rowNum="15"
        rownumbers="true"
    >
        <sjg:gridColumn name="id" index="id" title="ID" formatter="integer" sortable="false"/>
        <sjg:gridColumn name="name" index="name" title="Filename" sortable="true"/>
        <sjg:gridColumn name="hoster" index="hoster" title="Hoster" sortable="true"/>
        <sjg:gridColumn name="percent" index="percent" title="Progress" formatter="integer" sortable="true"/>
    </sjg:grid>





</body>
</html>
</jsp:root>