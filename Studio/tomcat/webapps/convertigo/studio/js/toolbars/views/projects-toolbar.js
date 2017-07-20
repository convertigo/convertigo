function ProjectsToolbar(container, projectsView) {
    Toolbar.call(this, container, "projects-action");

    // Refresh action
    this.addAction(
        "refresh-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/refresh.gif"),
        "Refresh",
        function () {
            // Refresh projects tree view
            projectsView.tree.jstree().refresh(true);
        }
    );

    // Save action
    this.addAction(
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
    );

    // Increase priority action
    this.addAction(
        "increase-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_increase_priority.gif"),
        "Increase selected object(s) priority",
        function () {
            ModalUtils.createMessageBox("Convertigo", "Not implemented.");
        }
    );

    // Decrease priority action
    this.addAction(
        "decrease-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_decrease_priority.gif"),
        "Decrease selected object(s) priority",
        function () {
            ModalUtils.createMessageBox("Convertigo", "Not implemented.");
        }
    );
}

ProjectsToolbar.prototype = Object.create(Toolbar.prototype);
ProjectsToolbar.prototype.constructor = ProjectsToolbar;
