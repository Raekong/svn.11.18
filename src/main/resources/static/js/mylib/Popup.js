define(
    ['myLayer'],
    function(layer){
        function Popup(title, dom, size, callback){
            this.title = title;
            this.dom = dom;
            this.size = size;

            var _that = this;
            this.pop = function(){
                var layIdnex = layer.layer.open({
                    title: false,
                    closeBtn: 1,
                    offset: '80px',
                    fixed: false,
                    type: 1,
                    content: dom.html(),
                    area: size,
                    success: function(layero, index){
                        callback(layero, index);
                    }
                });

                this.layIdnex = layIdnex;
            }


            this.close = function(){
                //console.log(_that.layIdnex);
                layer.layer.close(_that.layIdnex);
            }
        }

        return Popup;
    }
)