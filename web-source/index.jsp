<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="da">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>DKCLARIN</title>
  </head>
  <body>
    <h1>CLARIN-DK tools (provisory UI, idea testing)</h1>
    
    <h2>Apply workflow to uploaded file(s)</h2>
    <h3>(Choose a goal)</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
		    <td><span id="item1a" style="font-weight: bold;" >Choose one or more files:</span></td>
			<td><input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" /></td>
		  </tr><tr>
			<td><input type="radio" name="action" value="batch" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" checked="checked" />Compound input</td>
		  </tr><tr>
			<td><span id="maila" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2a" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" checked="checked" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <h2>Apply workflow to typed-in text only</h2>
    <h3>(Choose a goal)</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				text area
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
  		  <tr><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td>
		  </tr><tr>
			<td><input type="radio" name="UIlanguage" value="da" checked="checked" />Danish</td>
			<td><input type="radio" name="UIlanguage" value="en" />English</td>
  		  </tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
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
			<td><input type="radio" name="language" value="da" checked="checked" />Danish</td>
			<td><input type="radio" name="language" value="en" />English</td>
		  </tr><tr>
			<td><span id="mailb" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2b" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
		</table>
		<input type="submit" value="create annotation" alt="create annotation for uploaded file, goal directed" />
	</form>

    <h2>Apply precooked workflow to typed-in text</h2>
    <h3>DASISH T5.5</h3>
	<form enctype="multipart/form-data" method="post" action="createByGoalChoice">
		<table>
		  <tr>
			<td> 
				text area
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
			<td><input type="radio" name="language" value="da" checked="checked" />Danish</td>
			<td><input type="radio" name="language" value="en" />English</td>
		  </tr><tr>
			<td><span id="mailbb" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2bb" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
			
		  </tr>
  		  <tr><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
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
			<td><input type="radio" name="action" value="batch" />Iterate over input</td>
			<td><input type="radio" name="action" value="dataset" checked="checked" />Compound input</td>
		  </tr><tr>
			<td><span id="mailc" style="font-weight: bold;" >Your email address:</span></td>
			<td><input name="mail2" type="text" size="30" id="mail2c" style="vertical-align: middle;" />
			(Notifications will be sent to this address.)</td>
		  </tr>
  		  <tr><td>Password:</td><td><input type="password" name="password" value="" alt="password"/></td></tr>
		</table>
     	<input type="submit" value="create annotation" alt="create annotation for uploaded file, tool directed" />
	</form>
    
    
    
    <h1>Administration of Tools</h1>
	<form enctype="application/x-www-form-urlencoded" method="get" action="register">
		Password:<input type="password" name="passwordAsHandle" value="" alt="password"/>
		Your email address:<input name="mail2" type="text" size="30" id="mail2d" style="vertical-align: middle;" />
		<input type="submit" value="register new tool" alt="Bracmat" />
	</form>
	<form enctype="application/x-www-form-urlencoded" method="get" action="update">
		Password:<input type="password" name="passwordAsHandle" value="" alt="password"/>
		Your email address:<input name="mail2" type="text" size="30" id="mail2e" style="vertical-align: middle;" />
		<input type="submit" value="update tool" alt="Bracmat" />
	</form>
	<form enctype="application/x-www-form-urlencoded" method="get" action="deposit">
		Password:<input type="password" name="passwordAsHandle" value="" alt="password"/>
		Your email address:<input name="mail2" type="text" size="30" id="mail2f" style="vertical-align: middle;" />
		<input type="submit" value="deposit tool" alt="Bracmat" />
	</form>
	<dl>
	  <dt><a href="register">register new tool</a></dt>
	  <dd>Register new tool.</dd>
	  <dt><a href="update">update tool</a></dt>
	  <dd>Edit data for already registered tool.</dd>
	  <dt><a href="deposit">deposit tool</a></dt>
	  <dd>Deposit data for already registered tool in repository.</dd>
	</dl>

    <h1>Development area</h1>
    <h2>Reload script</h2>
	<form enctype="application/x-www-form-urlencoded" method="get" action="reloadScript">
		Password:<input type="password" name="password" value="" alt="password"/>
		<input type="submit" value="Bracmat" alt="Bracmat" />
	</form>

    <h2>Set language</h2>
    <p>User interface can be set to Danish or English</p>
	<form enctype="multipart/form-data" method="get" action="setLanguage">
		<p>
			<span id="langf" style="font-weight: bold;" >Choose a language: </span>
			<select name="UIlanguage">
                <option id="dan" value="da">Danish</option>
                <option id="eng" value="en">English</option>
			</select><br />
			<input type="submit" value="set language" alt="" />
		</p>
	</form>

    <h2>Stress test</h2>
    <p> Press the "stresstest"-button from several browser windows at the same time. Use different values.</p>
	<form enctype="multipart/form-data" method="get" action="stresstest">
		<p>
			<span id="numbr" style="font-weight: bold;" >Enter a positive number (&lt; 1000)</span>
			<input name="stress" type="text" size="30" id="stress" style="vertical-align: middle;" />
			<input type="submit" value="stresstest" alt="" />
		</p>
	</form>

    <h2>Evaluate program code</h2>
	<form enctype="application/x-www-form-urlencoded" method="get" action="bracmatevaluator">
		<textarea style="font-size: 9pt;" name="expression" rows="10" cols="80">{ Enter Bracmat expression to evaluate
  (Programmers only) }

</textarea><br />
		Password:<input type="password" name="password" value="" alt="password"/>
		<input type="submit" value="Bracmat" alt="Bracmat" />
	</form>


    <!--h2>Asynchronous requests</h2>
    <p>Emulate tool uploading result asynchronously</p>
	<form enctype="multipart/form-data" method="post" action="upload">
		<p>
			<input type="file" name="input" value="" size="50" alt="choose a file" /><br />
			<span id="itemh" style="font-weight: bold;" >Job ID:</span>
			<input name="job" type="text" size="30" id="job" style="vertical-align: middle;" /><br />
			<input type="submit" value="upload" alt="upload result from tool to Tools" />
		</p>
	</form-->
	
  </body>
</html>
