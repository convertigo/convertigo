export const developperTheme = {
    name: 'developper-theme',
    properties: {
		// =~= Theme Properties =~=
		"--theme-font-family-base": `Inter Variable, sans-serif`,
		"--theme-font-family-heading": `Inter Variable, sans-serif`,
		"--theme-font-color-base": "0 0 0",
		"--theme-font-color-dark": "255 255 255",
		"--theme-rounded-base": "9999px",
		"--theme-rounded-container": "8px",
		"--theme-border-base": "1px",
		// =~= Theme On-X Colors =~=
		"--on-primary": "0 0 0",
		"--on-secondary": "0 0 0",
		"--on-tertiary": "255 255 255",
		"--on-success": "0 0 0",
		"--on-warning": "0 0 0",
		"--on-error": "0 0 0",
		"--on-surface": "255 255 255",
        "--on-apprail": "255 255 255",
		// =~= Theme Colors  =~=
		// primary | #04CFFF 
		"--color-primary-50": "217 248 255", // #d9f8ff
		"--color-primary-100": "205 245 255", // #cdf5ff
		"--color-primary-200": "192 243 255", // #c0f3ff
		"--color-primary-300": "155 236 255", // #9becff
		"--color-primary-400": "79 221 255", // #4fddff
		"--color-primary-500": "4 207 255", // #04CFFF
		"--color-primary-600": "4 186 230", // #04bae6
		"--color-primary-700": "3 155 191", // #039bbf
		"--color-primary-800": "2 124 153", // #027c99
		"--color-primary-900": "2 101 125", // #02657d
		// secondary | #8FA1B3 
		"--color-secondary-50": "238 241 244", // #eef1f4
		"--color-secondary-100": "233 236 240", // #e9ecf0
		"--color-secondary-200": "227 232 236", // #e3e8ec
		"--color-secondary-300": "210 217 225", // #d2d9e1
		"--color-secondary-400": "177 189 202", // #b1bdca
		"--color-secondary-500": "143 161 179", // #8FA1B3
		"--color-secondary-600": "129 145 161", // #8191a1
		"--color-secondary-700": "107 121 134", // #6b7986
		"--color-secondary-800": "86 97 107", // #56616b
		"--color-secondary-900": "70 79 88", // #464f58
		// tertiary | #2E3D50 
		"--color-tertiary-50": "224 226 229", // #e0e2e5
		"--color-tertiary-100": "213 216 220", // #d5d8dc
		"--color-tertiary-200": "203 207 211", // #cbcfd3
		"--color-tertiary-300": "171 177 185", // #abb1b9
		"--color-tertiary-400": "109 119 133", // #6d7785
		"--color-tertiary-500": "46 61 80", // #2E3D50
		"--color-tertiary-600": "41 55 72", // #293748
		"--color-tertiary-700": "35 46 60", // #232e3c
		"--color-tertiary-800": "28 37 48", // #1c2530
		"--color-tertiary-900": "23 30 39", // #171e27
		// success | #4CAF50 
		"--color-success-50": "228 243 229", // #e4f3e5
		"--color-success-100": "219 239 220", // #dbefdc
		"--color-success-200": "210 235 211", // #d2ebd3
		"--color-success-300": "183 223 185", // #b7dfb9
		"--color-success-400": "130 199 133", // #82c785
		"--color-success-500": "76 175 80", // #4CAF50
		"--color-success-600": "68 158 72", // #449e48
		"--color-success-700": "57 131 60", // #39833c
		"--color-success-800": "46 105 48", // #2e6930
		"--color-success-900": "37 86 39", // #255627
		// warning | #FF9800 
		"--color-warning-50": "255 240 217", // #fff0d9
		"--color-warning-100": "255 234 204", // #ffeacc
		"--color-warning-200": "255 229 191", // #ffe5bf
		"--color-warning-300": "255 214 153", // #ffd699
		"--color-warning-400": "255 183 77", // #ffb74d
		"--color-warning-500": "255 152 0", // #FF9800
		"--color-warning-600": "230 137 0", // #e68900
		"--color-warning-700": "191 114 0", // #bf7200
		"--color-warning-800": "153 91 0", // #995b00
		"--color-warning-900": "125 74 0", // #7d4a00
		// error | #F44336 
		"--color-error-50": "253 227 225", // #fde3e1
		"--color-error-100": "253 217 215", // #fdd9d7
		"--color-error-200": "252 208 205", // #fcd0cd
		"--color-error-300": "251 180 175", // #fbb4af
		"--color-error-400": "247 123 114", // #f77b72
		"--color-error-500": "244 67 54", // #F44336
		"--color-error-600": "220 60 49", // #dc3c31
		"--color-error-700": "183 50 41", // #b73229
		"--color-error-800": "146 40 32", // #922820
		"--color-error-900": "120 33 26", // #78211a
		// surface | #1C242F 
		"--color-surface-50": "221 222 224", // #dddee0
		"--color-surface-100": "210 211 213", // #d2d3d5
		"--color-surface-200": "198 200 203", // #c6c8cb
		"--color-surface-300": "164 167 172", // #a4a7ac
		"--color-surface-400": "96 102 109", // #60666d
		"--color-surface-500": "28 36 47", // #1C242F
		"--color-surface-600": "25 32 42", // #19202a
		"--color-surface-700": "21 27 35", // #151b23
		"--color-surface-800": "17 22 28", // #11161c
		"--color-surface-900": "14 18 23", // #0e1217

        // =~= Theme Components =~=
        "--color-apprail-50": "225 226 227", // #e1e2e3
		"--color-apprail-100": "216 216 217", // #d8d8d9
		"--color-apprail-200": "206 207 208", // #cecfd0
		"--color-apprail-300": "176 177 179", // #b0b1b3
		"--color-apprail-400": "117 119 122", // #75777a
		"--color-apprail-500": "58 61 65", // #3A3D41
		"--color-apprail-600": "52 55 59", // #34373b
		"--color-apprail-700": "44 46 49", // #2c2e31
		"--color-apprail-800": "35 37 39", // #232527
		"--color-apprail-900": "28 30 32", // #1c1e20
		
	}
}