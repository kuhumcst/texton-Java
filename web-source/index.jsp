<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="da">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>DKCLARIN</title>
  </head>
  <body bgcolor="#C6D8D9">
    <h1>Automatic Navigation through Language Technology</h1>
    
    <h2>Apply LT workflow to uploaded file(s)</h2>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
		    <td><span id="item1a" style="font-weight: bold;" >Choose one or more files:</span></td>
			<td><input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" /></td>
		  </tr><tr>
			<td><input type="radio" name="action" value="batch" checked="checked" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" />Compound input</td>
		  </tr><tr>
			<td><span id="maila" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2a" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <h2>Apply workflow to typed-in text only</h2>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				Text to process:
			</td>
			<td>
				<textarea name="text" rows="4" cols="60"></textarea>
			</td>
		  </tr><tr>
			<td><input type="hidden" name="action" value="dataset" checked="checked" />Compound input</td>
		  </tr><tr>
			<td><span id="mailaa" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2aa" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <!-- h2>Apply workflow to data behind URL(s) (POST method)</h2>
    <h3>(Choose a goal)</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				URLs (max 3)
			</td>
			<td>
				<input name="URL" type="text" id="url1" size="60" />
				<input name="URL" type="text" id="url2" size="60" />
				<input name="URL" type="text" id="url3" size="60" />
			</td>
		  </tr><tr>
			<td><input type="hidden" name="action" value="batch" checked="checked" />Iterate over input</td>
		  </tr><tr>
			<td><span id="mailaa" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2aa" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form -->

    <h2>Apply workflow to data behind URL(s)</h2>
    <h3>(Choose a goal)</h3>
	<form method="get" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				URLs (max 3)
			</td>
			<td>
				<input name="URL" type="text" id="url1" size="60" />
				<input name="URL" type="text" id="url2" size="60" />
				<input name="URL" type="text" id="url3" size="60" />
			</td>
		  </tr><tr>
			<td><input type="hidden" name="action" value="batch" checked="checked" />Iterate over input</td>
		  </tr><tr>
			<td><span id="mailaa" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2aa" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <!-- h2>Create CMDI metadata only (POST method)</h2>
	<form enctype="multipart/form-data" method="post" action="createMetadataOnly">
		<table>
		  <tr>
			<td><span id="mailaa" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2aa" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create CMDI metadata" alt="create CMDI metadata" />
	</form -->

    <h2>Create CMDI metadata only</h2>
	<form method="get" action="createMetadataOnly">
		<table>
		  <tr>
			<td><span id="mailaa" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2aa" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create CMDI metadata" alt="create CMDI metadata" />
	</form>

    <h2>Apply precooked workflow to uploaded file</h2>
    <h3>DASISH T5.5</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
		    <td><span id="item1b" style="font-weight: bold;" >Choose a file:</span></td>
			<td><input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" /></td>
		  </tr><tr>
			<td><input type="radio" name="action" value="batch" checked="checked" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" />Compound input</td>
		  </tr><tr>
			<td><input type="radio" name="wrkflw" value="NER" checked="checked" />NER (Danish)</td>
			<td><input type="radio" name="wrkflw" value="CLEANING" />Cleaning</td>
		  </tr><tr>
			<td><input type="radio" name="outputformat" value="TEI" checked="checked" />TEI</td>
			<td><input type="radio" name="outputformat" value="flat" />flat</td>
		  </tr><tr>
			<td><input type="radio" name="language" value="da" />Danish</td>
			<td><input type="radio" name="language" value="en" checked="checked" />English</td>
		  </tr><tr>
			<td><span id="mailb" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2b" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <h2>Apply precooked workflow to typed-in text</h2>
    <h3>DASISH T5.5</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				Text to process:
			</td>
			<td>
				<textarea name="text" rows="4" cols="60"></textarea>
			</td>
		  </tr><tr>
			<td><input type="radio" name="action" value="batch" checked="checked" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" />Compound input</td>
		  </tr><tr>
			<td><input type="radio" name="wrkflw" value="NER" checked="checked" />NER (Danish)</td>
			<td><input type="radio" name="wrkflw" value="CLEANING" />Cleaning</td>
		  </tr><tr>
			<td><input type="radio" name="outputformat" value="TEI" checked="checked" />TEI</td>
			<td><input type="radio" name="outputformat" value="flat" />flat</td>
		  </tr><tr>
			<td><input type="radio" name="language" value="da" />Danish</td>
			<td><input type="radio" name="language" value="en" checked="checked" />English</td>
		  </tr><tr>
			<td><span id="mailbb" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2bb" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <h2>Apply single tool to uploaded file(s)</h2>
	<form enctype="multipart/form-data" method="post" action="createByToolChoice">
		<table>
		  <tr>
		    <td><span id="item1c" style="font-weight: bold;" >Choose one or more files:</span></td>
			<td><input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" /></td>
		  </tr><tr>
			<td><input type="radio" name="action" value="batch" checked="checked" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" />Compound input</td>
		  </tr><tr>
			<td><span id="mailc" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2c" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
		  </tr>
  		  <tr style="display:none;"><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" checked="checked" />English</td>
  		  </tr>
		</table>
     	<input type="submit" value="create annotation" alt="create annotation for uploaded file, tool directed" />
	</form>
    
  </body>
</html>
