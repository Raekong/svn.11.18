define(
    ["homecommon", "widget",  "template", "listadapt"],
    function( hc, widget, template, adapt){
        function List( container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#3').addClass('current');
                }
            );
            this.widget = new widget();
            this.widget.init( this.container.find('#tabs'), 'tab');
            this.lang = $('#frame').attr('i18n');

            this.adapt = new adapt( this.container ).init(container.find('.querynum .checked').attr('num'));
            
            var _that = this;

            this.init = function(){
                //改变分页数重新查询加载
                _that.container.find('.querynum .nocheck').click(
                    function(){
                        $(this).closest('.querynum').find('.nocheck').removeClass('checked');
                        $(this).addClass('checked');

                        var type = $(this).closest('.tabDiv').attr('tag');
                        var num = $(this).attr('num');

                        if( type == 'editor'){
                            _that.adapt.load(num);
                        }else{
                            _that.loadList(type, num);
                        }
                    }
                )
                
                //打开页面时初次加载
                _that.container.find('li.mytab').each(
                    function(){ //初始化加载各卡片页，启动查询
                        var type = $(this).attr("data");
                        var num = _that.container.find('[tag='+$(this).attr('data')+']').find('.querynum .checked').attr("num");
                        if( type == 'editor'){
                           //EDITOR在ADAPT初始化中加载了
                        }else{
                            _that.loadList(type, num);
                        }
                    }
                )
                //判断是否加载完成
                setTimeout(
                    _that.loadEnd,
                    200
                )
            }
            
            this.loadEnd = function(){
                var flag = true;
                _that.container.find('li.mytab').each(
                    function(){
                        if( !$(this).attr('loaded') ){
                            flag = false;
                        }
                    }
                )
                if( flag ){
                    _that.container.find('#loading').hide();
                    _that.container.find('#tabs').show();
                    var tab = _that.container.find('li.mytab:not(.empty)');
                    tab.eq(0).find('a').click();
                    
                }else{
                    setTimeout(
                        _that.loadEnd,
                        200
                    )
                }
            }

            
            //=======---------LOAD LIST------------------------------------------------
            this.loadEditorList = function( num ){
                // new listboard(_that.container, num).init();
                // new queryboard(_that.container.find("#configboards")).init();
            }

            this.loadList = function(type, num){
                    _that.widget.getPage()(
                        _that.lang=='true' ? 'zh' : 'en',
                        template,
                        _that.container.find('#'+type),
                        '/article/list/',
                        {type:type},
                        num,
                        function(array){
                            if( array.length == 0){
                               _that.container.find('li[data='+type+']').addClass('empty').hide(); //关闭没有论文的卡片页
                               _that.container.find('.tabDiv[tag='+type+']').hide();
                            }                                    
                            array.forEach(
                                function(a){
                                    a.js = a.journal + "[" + a.section + "]";
                                    a.authorstr = '';
                                    a.authors.forEach(
                                        function(aa){
                                            a.authorstr += aa.name+"["+aa.email+"]; "
                                        }
                                    );
                                    a.timeStamp = new Date(a.timeStamp).format("yyyy-MM-dd hh:mm:ss"); ;
                                    
                                }
                            )
                            _that.container.find('li[data='+type+']').attr('loaded', 'true');
                        },
                        function(dom){
                            dom.find('.row span.widow').click(
                                function(){
                                    var id = $(this).closest('.row').attr('id');
                                    window.open('/article/'+id);  
                                }
                            )
                        },
                        'id'
                    );
            }
            
            Date.prototype.format = function(format) {
                /*
                 * eg:format="yyyy-MM-dd hh:mm:ss";
                 */
                var o = {
                    "M+" : this.getMonth() + 1, // month
                    "d+" : this.getDate(), // day
                    "h+" : this.getHours(), // hour
                    "m+" : this.getMinutes(), // minute
                    "s+" : this.getSeconds(), // second
                    "q+" : Math.floor((this.getMonth() + 3) / 3), // quarter
                    "S+" : this.getMilliseconds()
                    // millisecond
                }
            
                if (/(y+)/.test(format)) {
                    format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4
                    - RegExp.$1.length));
                }
            
                for (var k in o) {
                    if (new RegExp("(" + k + ")").test(format)) {
                    var formatStr="";
                    for(var i=1;i<=RegExp.$1.length;i++){
                        formatStr+="0";
                    }
            
                    var replaceStr="";
                    if(RegExp.$1.length == 1){
                        replaceStr=o[k];
                    }else{
                        formatStr=formatStr+o[k];
                        var index=("" + o[k]).length;
                        formatStr=formatStr.substr(index);
                        replaceStr=formatStr;
                    }
                    format = format.replace(RegExp.$1, replaceStr);
                    }
                }
                return format;
            }

        }

        return List
    }
)