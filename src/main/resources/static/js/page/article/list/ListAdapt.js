define(
    ["listquery", "listshowconfig", "eventcenter"],
    function(lq, cf, ec){
        function ListAdapt( container){
            this.name = "ListAdapt";
            this.ec = new ec('debug');
            this.ec.addHandler( this ); 

            this.container = container;
            this.num = null;
            
            this.config = new cf(container, this.ec);

            this.lq = new lq(container, this.ec);
            
            var _that = this;
            this.init = function( num ){
                this.num = num;
                //初始化按键控件
                if( !this.container.find('[tag="editor"]').hasClass('empty') ){
                    this.container.find('[tag="editor"]>span').click(
                        this.editListConfig
                    )
                }

                this.config.initShowConfig(function(){
                    _that.lq.query( _that.num ); //在显示面板，以及显示内容设置完成后，执行回调，初始化无条件查询
                });

                this.lq.init();
                return this;
            }

            this.load = function( num ){
                this.num = num;
                this.lq.query( num ); //外界接口，更新分页数或有条件查询
            }

            this.queryWithCond = function(){
                _that.lq.query(_that.num);
            }

            this.editListConfig = function(event){
                if($(this).hasClass('setting')){
                    if($('#configboards:visible')){ 
                        $('[tag="editor"]>span').removeClass('setting');
                        $('#configboards').hide();
                        $('[data="editor"] a').css('background', '#fff');
                    };
                    return;
                } 
                var type = $(this).attr('class');
                
                $('[data="editor"] a').css('background', '#eeeeee');
                $('[data="editor"] a>span').removeClass('setting');
                $(this).addClass('setting');
                $('#configboards [tag]').hide();
                $('#configboards').hide();
                switch(type){
                    case 'search':
                        $('#configboards [tag="search"]').show();
                        break;
                    case 'message':
                        break;
                    case 'showconf':
                        //console.log($('#configboards [tag="showconf"]'));
                        $('#configboards [tag="showconf"]').show();
                        break;
                }
                $('#configboards').show();
                event.stopPropagation();    
            }

            this.exports = {
                'queryWithCond' : this.queryWithCond

            }
        }
        return ListAdapt;
    }
)