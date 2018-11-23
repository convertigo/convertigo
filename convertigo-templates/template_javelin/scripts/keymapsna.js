/*
 * Copyright (c) 1999-2016 Convertigo SA. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission of Convertigo sarl or in accordance with the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes no  representations  or  warranties  about   the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */


/*
 * $Workfile: keymapsna.js $
 * $Author: Davidm $
 * $Modifications jmc $
 * $Revision: 9 $
 * $Date: 16/03/21 11:04 $
 */

//	keycode	,doAction?	,!shift					,shift
var keymap_func=[
	[9		,false		,focusNextField			,focusPrevField			], // Tabulation
	[13		,true		,'KEY_ENTER'			,'KEY_ENTER'			], // Entr�e
//	[17		,false		,eraseToEnd				,eraseToEnd				], // Ctrl (gauche et droite)
	[27		,true		,'KEY_ATTN'				,'KEY_ATTN'				], // Echap
	[33		,true		,'KEY_ROLLUP'			,'KEY_ROLLUP'			], // Page pr�c�dente
	[34		,true		,'KEY_ROLLDOWN'			,'KEY_ROLLDOWN'			], // Page suivante
	[38		,false		,focusPrevField			,focusPrevField			], // Fl�che haut
	[40		,false		,focusNextField			,focusNextField			], // Fl�che bas
	[45		,false		,toggleInsertMode		,toggleInsertMode		], // Inser
	[106	,true		,focusNextField			,focusNextField			], // * du pav� num�rique
	[107	,false		,eraseToEnd				,eraseToEnd				], // + du pav� num�rique
	[109	,true		,'KEY_FIELDMINUS'		,'KEY_FIELDMINUS'		], // - du pav� num�rique	
	[110	,true		,'@,'					,'@,'					], // . du pav� num�rique
	[112	,true		,'KEY_PF1'				,'KEY_PF13'				], // F1
	[113	,true		,'KEY_PF2'				,'KEY_PF14'				], // F2
	[114	,true		,'KEY_PF3'				,'KEY_PF15'				], // F3
	[115	,true		,'KEY_PF4'				,'KEY_PF16'				], // F4
	[116	,true		,'KEY_PF5'				,'KEY_PF17'				], // F5
	[117	,true		,'KEY_PF6'				,'KEY_PF18'				], // F6
	[118	,true		,'KEY_PF7'				,'KEY_PF19'				], // F7
	[119	,true		,'KEY_PF8'				,'KEY_PF20'				], // F8
	[120	,true		,'KEY_PF9'				,'KEY_PF21'				], // F9
	[121	,true		,'KEY_PF10'				,'KEY_PF22'				], // F10
	[122	,true		,'KEY_PF11'				,'KEY_PF23'				], // F11
	[123	,true		,'KEY_PF12'				,'KEY_PF24'				]  // F12
];
