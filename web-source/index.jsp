<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="da">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>TEXT TONSORIUM</title>
    <style>
        h1, h2, h3, span, p, td, input, legend {
            font-family: "Comic Sans MS", cursive, sans-serif;
        }
    </style>
</head>
<body bgcolor="#F5F5F5">
    <h1>Text Tonsorium</h1>
    <p>
        Texts in many languages and formats (plain, word, pdf, html and even images) are welcome in our salon de beaut&eacute;.<br />
        If possible, our robot will apply cuts, markups, extensions and annotations according to your specifications.<br />
        This service is free for single texts and for smaller groups of texts, but please do not mix languages or formats.<br />
    </p>
    <form enctype="multipart/form-data" method="post" action="createByGoalChoice">
        <fieldset style="background-color:rgb(255,235,235)">
            <legend style="font-weight: bold;">Upload file(s)</legend>
            <table>
                <tr>
                    <td>

                        <span id="item1a">Choose one or more files:</span>
                    </td>
                    <td>
                        <input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" />
                    </td>
                </tr>
            </table>
        </fieldset>
		<p><strong><small>... or ...</small></strong></p>
        <fieldset style="background-color:rgb(235,235,255)">
            <legend style="font-weight: bold;">Enter up to three addresses of web pages</legend>
            <table>
                <tr>
                    <td>
                        <span id="item3a">URLs (full addresses, starting <br/>
						                  with http:// or https://</span>
                    </td>
                    <td>
                        <input name="URL" type="text" id="url1" size="87" /><br />
                        <input name="URL" type="text" id="url2" size="87" /><br />
                        <input name="URL" type="text" id="url3" size="87" />
                    </td>
                </tr>
            </table>
        </fieldset>
		<p><strong><small>... or ...</small></strong></p>
        <fieldset style="background-color:rgb(235,255,235)">
            <legend style="font-weight: bold;">Type or cut-and-paste some text</legend>
            <table>
                <tr>
                    <td>
                        <span id="item2a">Text to process:</span>
                    </td>
                    <td><textarea name="text" rows="4" cols="84"></textarea></td>
                </tr>
            </table>
        </fieldset>
        <table>
            <tr style="display:none;">
                <td>Password:</td>
                <td><input type="password" name="password" value="" alt="password" /></td>
            </tr>
            <tr>
                <td>Language of the user interface</td>
                <td>
                    <input type="radio" name="UIlanguage" value="en" checked="checked" />English<br />
                    <input type="radio" name="UIlanguage" value="da" />Danish
                </td>
            </tr>
			<tr>
			<td>Next:</td>
			<td><input type="hidden" name="action" value="batch" checked="checked" />
        <input type="submit" value="Specify the required result" alt="create annotation, goal directed" /></td>
			</tr>
        </table>
        <p>
            <small>Although we will do our best to protect your data, we cannot guarantee the security of your data transmitted to our site.<br/>
			Use this service is at your own risk!<br />
			This workflow manager is open source and can be downloaded from <a href="https://github.com/kuhumcst/DK-ClarinTools">https://github.com/kuhumcst/DK-ClarinTools</a>.</small>
        </p>
    </form>
</body>
</html>
