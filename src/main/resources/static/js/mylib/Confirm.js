define(
    ['myLayer'],
    function(layer){
        function Confirm(title, msg, i18n, callback, size){
            this.title = title;
            this.i18n = i18n;
            this.size = (!!size ?  size : '368px') ;

            var _that = this;

            this.confirm = function(){
                $.post('/component/confirm/'+this.i18n, function(rst){
                    var dom = $(rst);
                    dom.find('#logo').text(title);
                    dom.find('#msg').text(msg);
                    var layIdnex = layer.layer.open({
                        title: false,
                        closeBtn: 1,
                        offset: '80px',
                        fixed: false,
                        type: 1,
                        content: dom.html(),
                        area: _that.size,
                        success: function(layero, index){
                            callback(layero, index);
                        }
                    });

                    _that.layIdnex = layIdnex;
                })
                
            }


            this.close = function(){
                layer.layer.close(_that.layIdnex);
            }
        }

        return Confirm;
    }
)