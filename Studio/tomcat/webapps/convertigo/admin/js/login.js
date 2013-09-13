jQuery().ready(
		function() {
		    checkAuthentication();
		    $(".index").show();

			$("#FieldConvAdminUserLogin").focus();

			$("#dlgAuthFailed").dialog( {
				autoOpen : false,
				title : "Error",
				modal : true,
				buttons : {
					Ok : function() {
						$(this).dialog('close');
						return false;
					}
				}
			});

			$("#loginForm").submit(
					function() {
						authenticate(
								$("#FieldConvAdminUserLogin").val(),
								$("#FieldConvAdminUserPassword").val());
						return false;
					});		
		});

function checkAuthentication() {
	$.ajax( {
		type : "POST",
		url : "services/engine.CheckAuthentication",
		dataType : "xml",
		data : {},
		success : function(xml) {
			var $xml = $(xml);
			var $authenticated = $xml.find("authenticated");
			if ($authenticated.text() == "true" && checkRoleAdmin($xml)) {
				document.location.href = "main.html";
			}
		}
	});
}

function authenticate(userName, password) {
	request = $.ajax( {
		type : "POST",
		url : "services/engine.Authenticate",
		data : {
			authUserName : userName,
			authPassword : password,
			authType : "login"
		},
		dataType : "xml",
		success : function(xml) {
			var $xml = $(xml);
			if ($xml.find("success").length > 0 && checkRoleAdmin($xml)) {
				$("form").unbind("submit").submit();
			} else {
				$("#dlgAuthFailed_message").html($xml.find("error").text().replace(/\n/g, "<br/>"));
				$("#dlgAuthFailed").dialog('open');
			};
		}
	});
}

function checkRoleAdmin($xml) {
	return $xml.find("roles>role[name=\"WEB_ADMIN\"]").length > 0;
}
