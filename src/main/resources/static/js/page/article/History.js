define(
    [],
    function(){
        function History( container ){
            this.container = container;

            var _that = this;

            this.init = function(){
                this.container.find('span[mid]').click(
                    function(){
                        var mid = $(this).attr('mid');
                        $.post(
                            '/article/history/' + mid,
                            function(rst){
                                _that.container.find("#histable").hide();
                                _that.container.find("#tpls").empty().append(rst).show();
                                _that.container.find('#cancel').click(
                                    function(){
                                        console.log("fadsfa");
                                        _that.container.find("#histable").show();
                                        _that.container.find("#tpls").hide();
                                    }
                                );
                            }
                        )
                    }
                )

                
            }

        }
        return History;
    }
)