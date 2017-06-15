function ProjectsToolbar(panelSelector, projectsView) {
    Toolbar.call(this, panelSelector);

    // Save action
    this.addAction(new ActionToolbar(
        "save-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/project_save.gif"),
        "Save",
        function () {
            var selectedNodeId = projectsView.tree.jstree().get_selected()[0];
            var projectNode = projectsView.getProjectNode(selectedNodeId);

            // Do not call Save service if no node is selected
            if (projectNode !== null) {
                $.ajax({
                    dataType: "xml",
                    url: Convertigo.createServiceUrl("studio.projects.Save"),
                    data: {
                        qname: projectNode.data.qname 
                    },
                    success: function (data, textStatus, jqXHR) {
                        // Show errors
                        $(data).find("admin").find(">*[name='MessageBoxResponse']").reverse().each(function() {
                            var $msgBoxXml = $(this).find(">*");
                            ModalUtils.createMessageBox(
                                $msgBoxXml.find("title").text(),
                                $msgBoxXml.find("message").text()
                            );
                        });
                    }
                });
            }
        }
    ));

    // Increase priority action
    this.addAction(new ActionToolbar(
        "increase-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_increase_priority.gif"),
        "Increase selected object(s) priority",
        function () {
        }
    ));

    // Decrease priority action
    this.addAction(new ActionToolbar(
        "decrease-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_decrease_priority.gif"),
        "Decrease selected object(s) priority",
        function () {
        }
    ));
}

ProjectsToolbar.prototype = Object.create(Toolbar.prototype);
ProjectsToolbar.prototype.constructor = ProjectsToolbar;
