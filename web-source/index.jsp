<%@page contentType="application/xhtml+xml"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.w3.org/MarkUp/SCHEMA/xhtml11.xsd" xml:lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Text Tonsorium - Natural Language Processing toolchains automatically composed and scheduled for you</title>
    <style type="text/css">
        h1, h2, h3, span, p, td, input, legend, textarea {
            font-family: "Comic Sans MS", cursive, sans-serif;
        }
        input, textarea {
            font-size: 12pt;
        }
        p {margin: 0}
        p.indent {text-indent: 20px}
		body { font-size: 100%; }
		fieldset {border-style: none;}
		td.textareawide {width:70%}
		.smallmargin {margin-top:0.5em; margin-bottom:0.5em;}
        /*Bold Blacks and Vibrant Highlights*/
		/*.bodycanvas {background-color:#F5F5F5}
		.fileupload {background-color:#EC576b}
		.URLs {background-color:#4EC5C1}
		.typein {background-color:#E5E338}*/
        /*Striking and Energetic
		.bodycanvas {background-color:#F2EEE2}
		.fileupload {background-color:#F5CE28}
		.typein {background-color:#F5CE28}
		.URLs {background-color:#F5CE28} */
        /*blue, gradient*/
		.bodycanvas {background: linear-gradient(rgb(254,254,254), rgba(0,210,255,0.2));background-repeat: no-repeat;}
		.fileupload {background-color:rgba(154,186,206,0.6)}
		.typein {background-color:rgba(154,186,206,0.6)}
		.URLs {background-color:rgba(154,186,206,0.6)}
        /*.typein {background-color:#43C0F6}
		.URLs {background-color:#F81B84}*/
    </style>
    <meta name="description" content="This NLP workflow managment system automatically combines the necessary natural language processing tools to achieve your goal, in a way similar to how a trip planner computes the best route from your current position to your destination. A very advanced application mostly written in the Bracmat programming language." />
</head>
<body class="bodycanvas">
<!--div  style="margin:0px auto;width:860px;text-align:left;border:1px solid #336699;"-->
<div  style="margin:0px auto;width:55em;text-align:left;">
    <h1 class="smallmargin">Text Tonsorium <!--&#128136;-->&nbsp;&nbsp;&nbsp; <small><small>A salon de beaut&eacute; for Natural Language Processing</small></small></h1>
    <p>
        Upload your input using the form on this page. On the next page you specify the desired final result -
        there are several annotation types, file formats, languages and other traits to choose from.
        The hard part - picking and orchestrating the Natural Language Processing tools that are needed to achieve your goal - is handled by this service.
    </p>
    <form enctype="multipart/form-data" method="post" action="createByGoalChoice">
        <fieldset class="fileupload">
            <legend style="font-weight: bold;">Upload one or more files</legend>
            <table>
                <tr>
                    <td>

                        <span id="item1a">Choose one or more files:</span>
                    </td>
                    <td>
                        <input type="file" name="input" value="" size="50" alt="choose one or more files" multiple="multiple" />
                    </td>
                    <td>
                        (Do not mix file formats.)
                    </td>
                </tr>
            </table>
        </fieldset>
		<p class="smallmargin"><strong><small>... or ...</small></strong></p>
        <fieldset class="URLs">
            <legend style="font-weight: bold;">Enter addresses of web pages</legend>
            <table>
                <tr>
                    <td>
                        <span id="item3a">One URL per line, each starting with http:// <br />or https://</span>
                    </td>
                    <td class="textareawide">
                        <textarea name="URLS" rows="3" cols="80" style="width:100%"></textarea>
                    </td>
                </tr>
            </table>
        </fieldset>
		<p class="smallmargin"><strong><small>... or ...</small></strong></p>
        <fieldset class="typein">
            <legend style="font-weight: bold;">Type or copy and paste some text</legend>
            <table>
                <tr>
                    <td>
                        <span id="item2a">Text to process:</span>
                    </td>
                    <td class="textareawide"><textarea name="text" rows="3" cols="80" style="width:100%"></textarea></td>
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
				<td style="width:20%"></td>
			<td>Next:<input type="hidden" name="action" value="batch" checked="checked" />
        <input type="submit" value="Specify the required result" alt="create annotation, goal directed" /></td>
			</tr>
        </table>
        <p>
            <small>
            This service is a workflow management system (WMS) that not only executes workflows, but also composes workflows from building blocks. 
            Each building block encapsulates a Natural Language Processing tool.
            </small>
        </p>
        <p class="indent">
            <small>
            The WMS may compose many workflows that all lead to your goal.
            It will then ask you to choose one of the proposed workflows.
            In general, the more detail you add to your goal, the fewer solutions the WMS will find, even zero.
            </small>
        </p>
        <p class="indent">
            <small>
            Find the most recent source code of the WMS on <a href="https://github.com/kuhumcst/DK-ClarinTools">GitHub</a>, where you can also contact us.
            </small>
        </p>
        <p class="indent">
            <small>
            This service is free for small amounts of text.
            Do not send sensitive data to this service and use it at your own risk!
            </small>
        </p>
    </form>
</div>
</body>
</html>
