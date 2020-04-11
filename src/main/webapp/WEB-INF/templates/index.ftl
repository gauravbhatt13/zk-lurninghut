<html>
<head>
  <title>Welcome!</title>
</head>
<body>
    <#list row as key, value>
      <#if key?contains("header")>
        <h1>${value}</h1>
      </#if>
      <#if key?contains("para")>
        <p>${value}</p>
      </#if>
      <#if key?contains("gist")>
        ${value}
      </#if>
      <#if key?contains("image")>
        <img src="data:image/png;base64, ${value}"/>
      </#if>
    </#list>
</body>
</html>