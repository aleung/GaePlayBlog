/*
Copyright (c) 2003-2010, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

CKEDITOR.editorConfig = function( config )
{
	// Define changes to default configuration here. For example:
	// config.language = 'fr';
	// config.uiColor = '#AADC6E';
	config.height = 500;
	config.startupOutlineBlocks = true;
	config.toolbar = 'Leo';
	config.toolbar_Leo = 
		[  
		    ['Source','Preview','-','Maximize','ShowBlocks','-','Templates'],
		    ['PasteText','Find'],
		    ['Link','Unlink','Anchor'],
		    ['Image','Table','HorizontalRule','SpecialChar'],
		    ['TextColor','BGColor'],
		    '/',
		    ['Bold','Italic','Underline','Strike','-','Subscript','Superscript'],
		    ['NumberedList','BulletedList','-','Outdent','Indent','Blockquote'],
		    ['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
		    ['Styles','Format','Font','FontSize'],
		    ['RemoveFormat'],
		];	
};
