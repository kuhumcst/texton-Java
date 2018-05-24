<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="da">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>TEXT TONSORIUM</title>
    <style>
        h1, h2, h3, span, p, td, input, legend, textarea {
            font-family: "Comic Sans MS", cursive, sans-serif;
        }
        input, textarea {
            font-size: 12pt;
        }
		body { font-size: 100%; }
		fieldset {border-style: none;}
		.smallmargin {margin-top:0.5em; margin-bottom:0.5em;}
        /*Bold Blacks and Vibrant Highlights*/
		/*.bodycanvas {background-color:#F5F5F5}
		.fileupload {background-color:#EC576b}
		.URLs {background-color:#4EC5C1}
		.typein {background-color:#E5E338}*/
        /*Striking and Energetic*/
		.bodycanvas {background-color:#F2EEE2}
		.fileupload {background-color:#F5CE28}
		.typein {background-color:#43C0F6}
		.URLs {background-color:#F81B84}
    </style>
</head>
<body class="bodycanvas">
<!--div  style="margin:0px auto;width:860px;text-align:left;border:1px solid #336699;"-->
<div  style="margin:0px auto;width:55em;text-align:left;">
    <h1 class="smallmargin">&#128136; Text Tonsorium &#128136;</h1>
    <p>
        Texts in many languages and formats (plain, word, pdf, html and even images) are welcome in our salon de beaut&eacute;.<br />
        If possible, our robot will apply cuts, markups, extensions and annotations according to your specifications.<br />
        This service is free for single texts and for smaller groups of texts, but please do not mix languages or formats.<br />
    </p>
    <form enctype="multipart/form-data" method="post" action="createByGoalChoice">
        <fieldset class="fileupload">
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
		<p class="smallmargin"><strong><small>... or ...</small></strong></p>
        <fieldset class="URLs">
            <legend style="font-weight: bold;">Enter up to three addresses of web pages</legend>
            <table>
                <tr>
                    <td>
                        <span id="item3a">URLs - full addresses, starting with http:// or https://</span>
                    </td>
                    <td width="70%">
                        <input name="URL" type="text" id="url1" size="50" /><br />
                        <input name="URL" type="text" id="url2" size="50" /><br />
                        <input name="URL" type="text" id="url3" size="50" />
                    </td>
                </tr>
            </table>
        </fieldset>
		<p class="smallmargin"><strong><small>... or ...</small></strong></p>
        <fieldset class="typein">
            <legend style="font-weight: bold;">Type or cut-and-paste some text</legend>
            <table>
                <tr>
                    <td>
                        <span id="item2a">Text to process:</span>
                    </td>
                    <td width="70%"><textarea name="text" rows="4" cols="80"></textarea></td>
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
				<td width="20%"></td>
			<td>Next:<input type="hidden" name="action" value="batch" checked="checked" />
        <input type="submit" value="Specify the required result" alt="create annotation, goal directed" /></td>
			</tr>
        </table>
        <p>
            <small>Although we will do our best to protect your data, we cannot guarantee the security of your data transmitted to our site.<br/>
			Use of this service is at your own risk!<br />
			This Workflow Management System is open source and can be downloaded from <a href="https://github.com/kuhumcst/DK-ClarinTools">https://github.com/kuhumcst/DK-ClarinTools</a>.</small>
        </p>
    </form>
</div>
</body>
</html>
