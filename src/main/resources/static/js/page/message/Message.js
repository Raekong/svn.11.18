define(
    ["homecommon", "widget", "eventcenter",  "popup", "template"],
    function( hc, widget, ec, popup, template){
        function Message(container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#4').addClass('current');
                }
            );

            this.lang = this.container.attr('i18n');
            this.widget = new widget();
            this.ec = new ec('debug');
            this.ec.addHandler(this);

            var _that = this;

            this.queryMessage = function(){
                var data = {};
                data.sender =  _that.container.find('#sender').val().trim();
                data.title = _that.container.find('#title').val().trim();
                data.content = _that.container.find('#content').val().trim();

                var size = _that.container.find('#querynum span.checked').attr('num');

                // var active = ["激活", "actived"];
                // var disactive = ["未激活", "disactived"];
                // var enable = ["正常", "enable"];
                // var disable = ["禁用", "disable"];
                _that.widget.getPage()(
                    _that.lang =='true' ? 'zh' : 'en',
                    template,
                    _that.container.find('#messageTable'),
                    '/message/search',
                    data,
                    size,
                    function(array){
                        //research result
                    },
                    function(rst){
                        rst.find('.row').click(function(){
                            var id = $(this).attr('id')
                            $.post(
                                '/message/getMessage',
                                { id:id },
                                function(res){
                                    $.post(
                                        '/message/message-pop',
                                        function(rst){
                                            var dom = $(rst);
                                            var title = dom.find('.title').text();
                                            _that.msgpop = new popup(title, dom,  '768px', function(dom, index){
                                               

                                                dom.find('#msg-title').val(res.title)
                                                dom.find('#msg-content').html(res.content.replace('\\n', "<br/>"))
                                                dom.find('#cancel').click(
                                                    function(){
                                                        _that.msgpop.close();
                                                    }
                                                );
                                            });
                                            _that.msgpop.pop();
                                            if(!!res.appendsJSONStr){
                                                _that.showFiles(res.appendsJSONStr);
                                            }
                                        }
                                    )
                                }
                            )

                        })
                    },
                    'id'
                )

            }

            this.init = function(){
                _that.container.find('#search').click(
                    _that.queryMessage
                )

                this.container.find('#querynum span.nocheck').click(
                    function(){
                        $(this).parent().find('.nocheck').removeClass('checked');
                        $(this).addClass('checked');
                    }
                )
            }

            this.fileTpl = '{{each files as file }}<div style="display: flex;"><div style="flex:1 1 20%; text-align:center;"><span class="nocheck" data="{{file.fileName}},{{ file.innerName }}"></span></div><div style="flex:1 1 80%;"><a href="{{file.path}}">{{file.fileName}}</a></div></div>{{/each}}';

            this.showFiles = function(appendsJSONStr){
                var file_list = $('#fileList')
                file_list.empty()
                var files = JSON.parse(appendsJSONStr);

                files.forEach(
                    function(d ){
                        d.innerName = d.path.split('/').reverse()[0];
                    }
                );

                var data = {}; data.files = files;
                var listdom = template.compile(_that.fileTpl)(data);
                file_list.append(listdom);

                file_list.find('span.nocheck').click(
                    function(){
                       if( $(this).hasClass('checked')) $(this).removeClass('checked');
                       else $(this).addClass('checked');
                    }
                )

                $('#downloadzip').click(
                    function(){
                        var selected = $('#fileList span.checked');
                        var tmp = [];
                        for(var i=0; i<selected.length; i++){
                            tmp.push(selected.eq(i).attr('data'));
                        }

                        var fileStr = tmp.join(";");
                        console.log(fileStr);
                        window.open("/message/download?appends="+fileStr+"&type=COS");
                    }
                )
            }

        }
        return Message;
    }
)