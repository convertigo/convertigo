function ProjectsToolbar(projectsView, panelSelector) {
    Toolbar.call(this, panelSelector);
    
    // Save action
    this.addAction(
        "save-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/project_save.gif"),
        "Save",
        function () {
            var selectedNodeId = projectsView.tree.jstree().get_selected()[0];
            var projectNode = projectsView.getProjectNode(selectedNodeId);
            $.ajax({
                url: Convertigo.createServiceUrl("studio.projects.Save"),
                data: {
                    qname: projectNode.data.qname 
                },
                success: function () {
                }
            });
        }
    );
    
    // Increase priority action
    this.addAction(
        "increase-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_increase_priority.gif"),
        "Increase selected object(s) priority",
        function () {
        }
    );
    
    // Decrease priority action
    this.addAction(
        "decrease-priority-action",
        Convertigo.createServiceUrl("studio.database_objects.GetMenuIcon?iconPath=icons/studio/dbo_decrease_priority.gif"),
        "Decrease selected object(s) priority",
        function () {
        }
    );
}

ProjectsToolbar.prototype = Object.create(Toolbar.prototype);
ProjectsToolbar.prototype.constructor = ProjectsToolbar;
