define(
    ["template"],
    function( template ){
        function ShowConfig(container, ec ){
            this.container = container;
            this.name = "ShowConfig";
            this.ec = ec;
            this.ec.addHandler( this )

            var _that = this;
            //=======-------------初始化显示面板-----------------------------
            this.initShowConfig = function( callback ){
                var configBoard = _that.container.find('#showconfig');
                
                //------- 保存用户配置---------------------------------------
                configBoard.find('#saveshowconfig').click(
                    _that.saveShowSetting
                )


                var tpl = '<div class="item" ><span class="nocheck" default="{{sys}}" data="{{key}}" table="{{point}}"></span><div>{{data}}</div></div>';
                var render = template.compile(tpl);
                $.post(
                    '/article/listboard/showconfig', //加载系统的默认显示项及其配置
                    function(rst){
                        var points = configBoard.find('[point]'); //此时，页面上用户可以查看的显示列已经渲染完成
                        points.each(
                            function(){
                                var key = $(this).attr('point');
                                var list = rst[key]; //取系统对该显示列的默认配置

                                var dom = $(this);
                                list.forEach(
                                    function(d){
                                        var data = {};
                                        data.key = d[0];
                                        data.point = key;
                                        data.data = d[1];
                                        data.sys = d[2];
                                        dom.append( render(data) ); //加入列中，完成系统的默认显示配置
                                    }
                                )
                            }
                        )
                        _that.sysShowSetting = rst;
                        _that.loadShowConfig(rst , callback); //开始加载用户的自定义显示配置，系统与自定义的配置数据项不一样的

                        configBoard.find('span.nocheck').click( //设置面板中各显示项是否显示按键事件
                            _that.adjustSetting
                        )
                    }
                )
            }

            this.saveShowSetting = function(){
                var configBoard = _that.container.find('#showconfig');
                var points = configBoard.find('[point]');
                var s = {};
                points.each(
                    function(){
                        var t = {};
                        $(this).find('span.nocheck').each(
                            function(){
                                var flag = $(this).hasClass('checked');
                                var data = $(this).attr('data');
                                t[data] = flag ? 'true' : 'false';
                            }
                        );
                        s[$(this).attr('point')] = t;
                    }
                )
                $.post(
                    '/article/listboard/saveShowConfig',
                    { settingJson :JSON.stringify(s) },
                    function(rst){
                        window.location.reload();
                    }
                )
            }

            this.adjustSetting = function(){
                //锁死系统默认显示项
                if( $(this).attr('default') == 'true') return;
                if( $(this).hasClass('checked')){
                    $(this).removeClass('checked');
                }else{
                    $(this).addClass('checked');
                }
            }

            this.loadShowConfig = function( sys, callback ){
                $.post(
                    '/article/listboard/loadShowConfig',
                    function(rst){
                        _that.userShowSetting = rst;
                        _that.initShowConfigSetting( !!rst? rst: sys, !!rst?false:true, callback);
                    }
                )
            }

            this.initShowConfigSetting = function(rst, isSystem, callback){
                var configBoard = this.container.find('#showconfig');
                /**
                 * 初始化SHOW TABLE 的LIST,如果不是系统默认的，则采用用户自定义的方式来实现
                 * 完全的注释掉了，因为现在可以访问的必须要显示，省得麻烦
                 
                if(!isSystem){
                    for(var key in rst){
                        // if(rst[key]['choice'] != 'true'){
                        //     //console.log(key);
                        //     _that.container.find("#editor [data="+ key +"]").remove();
                        // }
                    }
                }*/

                /**
                 * 初始化CONFIG BOARD
                 */
                if( isSystem){ //装载系统默认定义
                    for(var key in rst){
                        var list = rst[key];
                        list.forEach(
                            function(d){
                                configBoard.find( '[table='+key+'][data='+ d[0] +']').addClass(
                                    isSystem && d[2]=='true' ? 'checked' : ''
                                );
                            }
                        )
                    }
                }else{
                    for(var key in rst){ //装载用户自定义
                        //console.log(rst[key]);
                        var list = configBoard.find('[point='+key+']');
                        for(var l in rst[key]){
                            list.find('[data='+ l +']').addClass( rst[key][l] == 'true' ? 'checked':'');
                        }
                    }
                }
                callback();
            }


            this.exports = {
                'sysShowConfig' : function(){
                    return _that.sysShowSetting
                }
            }

        }
        return ShowConfig;
    }
)