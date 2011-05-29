<?xml version="1.0" encoding="ISO-8859-1" ?>
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" version="2.0">
	<jsp:directive.page contentType="text/html; charset=ISO-8859-1" 
		pageEncoding="ISO-8859-1" session="false"/>
	<jsp:output doctype-root-element="html"
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
		omit-xml-declaration="true" />

<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="sj" uri="/struts-jquery-tags"%>
<%@ taglib prefix="sjg" uri="/struts-jquery-grid-tags"%>
		
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <sj:head jqueryui="true" jquerytheme="cupertino"/>
    
	<title>Insert title here</title>
</head>
<body>

    <s:form id="form" theme="xhtml">
      <sj:datepicker id="date0" label="Select a Date" />

	</s:form>
</body>
</html>
</jsp:root>