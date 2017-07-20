var EditorsManager = {
    editors: {
    },
    get: function (qname) {
        return this.editors[qname];
    },
    put: function (qname, editor) {
        this.editors[qname] = editor;
    }
};
