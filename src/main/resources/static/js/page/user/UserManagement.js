define(
    ["homecommon", "widget", "eventcenter", "datacenter",  "popup", "template", "msgbox"],
    function( hc, widget, ec, dc,  popup, template, msgbox ){
        function UserManagement(container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#6').addClass('current');
                }
            );

            this.name = 'USERMANAGER';

            this.lang = this.container.attr('i18n');
            this.widget = new widget();

            this.ec = new ec('debug');
            this.ec.addHandler(this);

            var _that = this;
            this.ds = {
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },
                'data-password': {
                    'verify': '',
                    'val' : '666666'
                },
                'data-first': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-middle': {
                    'verify': '',
                    'val' : ''
                },
                'data-last': {
                    'verify': '',
                    'val' : ''
                }
            }

            this.createUser = function(){
                $.post(
                    '/user/create-user-pop',
                    function(rst){
                        var dom = $(rst);
                        var inputCells = dom.find("div[cell]");
                        inputCells.each(
                            function(){
                                _that.widget.init($(this), 'input');
                            }
                        );

                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  ['568px'], function(dom, index){
                            var sdc = new dc( dom, _that.ds );

                            dom.find("#cancel").click(
                                function(){
                                    _that.pop.close();
                            })

                            dom.find("#save").click(
                                function(){
                                    var data = sdc.exec();
                                    dom.find('.error').removeClass('error');

                                    if( !!data.verify.key ){
                                        data.verify.dom.addClass('error');
                                        if( data.verify.key.indexOf('email') != -1 ){
                                            dom.find('.errorinfo').text('请填写合法的邮箱地址格式')
                                        }
                                        return;
                                    }

                                    $.post(
                                        '/user/create',
                                        data.data,
                                        function( rst ){
                                            if( rst.userId == -1 ){
                                                _that.widget.msgbox(_that.lang=='true', 'info', 'Message', 'The email of the created user has already been registered.');
                                            }else{
                                                _that.widget.msgbox(_that.lang=='true', 'info', 'Message', 'User has created successfully');
                                            }
                                             
                                        }
                                    )
                            })
                        })
                        _that.pop.pop();
                    }
                )
            }

            this.queryUser = function(){
                var data = {};
                var role =  _that.container.find('#rolelist').val();
                if( !!role ){
                    data.rid = _that.container.find('#rolelist').attr('rid');
                }else{
                    data.rid = -1;
                }

                data.name = _that.container.find('#username').val().trim();
                data.email = _that.container.find('#useremail').val().trim();

                var size = _that.container.find('#querynum span.checked').attr('num');

                var active = ["激活", "actived"];
                var disactive = ["未激活", "disactived"];
                var enable = ["正常", "enable"];
                var disable = ["禁用", "disable"];
                var info = ["消息", "Message"];
                var content = ["用户密码已重置为666666", "Password of the user has been reset to 666666 "];

                _that.widget.getPage()(
                    _that.lang=='true' ? 'zh' : 'en',
                    template,
                    _that.container.find('#userTable'),
                    '/user/queryUserWithRidAndNameAndEmail',
                    data,
                    size,
                    function(array){
                        array.forEach(
                            function(d){
                                if( !d.username ){
                                    d.username  = '-';
                                }
                                if( d.actived ){
                                    d.actived = ( _that.lang=='true' ? active[0] : active[1])
                                }else{
                                    d.actived = ( _that.lang== 'true' ? disactive[0] : disactive[1])
                                }

                                if( d.disabled ){
                                    d.disabled = ( _that.lang== 'true' ? disable[0] : disable[1])
                                }else{
                                    d.disabled = ( _that.lang== 'true' ? enable[0] : enable[1])
                                }
                            }
                        )
                    },
                    function(rst){
                        //bind operation;
                        rst.find('.disable').click(
                            function(){
                                console.log('disable')
                                var id = $(this).parent().parent().attr('userid');
                                $.post(
                                    '/user/disable',
                                    {id:id},
                                    function(rst){
                                        _that.queryUser();
                                    }
                                )
                            }
                        )
                        rst.find('.loginas').click(
                            function(){
                                console.log('loginas')
                                var id = $(this).parent().parent().attr('userid');
                                $.post(
                                    '/user/turn',
                                    {id:id},
                                    function(rst){
                                        _that.queryUser();
                                    }
                                )
                            }
                        )
                        rst.find('.active').click(
                            function(){
                                console.log('active')
                                var id = $(this).parent().parent().attr('userid');
                                $.post(
                                    '/user/active',
                                    { id:id},
                                    function(rst){
                                        _that.queryUser();
                                    }
                                )
                            }
                        )
                        rst.find('.resetpassword').click(
                            function(){
                                var id = $(this).parent().parent().attr('userid');
                                $.post(
                                    '/user/setPassword666',
                                    { id:id },
                                    function(rst){
                                        var index = ( _that.lang=='true' ? 0 : 1);
                                        _that.widget.msgbox(_that.lang=='true', 'info', info[index],content[index]);
                                    }
                                )
                            }
                        );
                        rst.find('.message').click(
                            function(){
                                var id = $(this).closest('.row').attr('userid');
                                var email =  $(this).closest('.row').find('[email]').attr('email');
                                var name =  $(this).closest('.row').find('[username]').attr('username');

                                _that.recevier = {};
                                _that.recevier.email = email; _that.recevier.name = name; _that.recevier.id = id;
                                $.post(
                                    '/user/user-message-pop',
                                    function(rst){
                                        var dom = $(rst);
                                        var title = dom.find('.title').text();
                                        _that.msgpop = new popup(title, dom,  '768px', function(dom, index){
                                            var height = $(dom).height();
                                            $(body).height(height + 120);

                                            dom.find('#recevier').text( name +' [ '+ email +' ]')
                                            var mb = new msgbox(_that.lang=='true'? 'zh':'en', dom, _that.ec);
                                            mb.init();

                                            dom.find('#cancel').click(
                                                function(){
                                                    _that.msgpop.close();
                                                }
                                            );
                                        });
                                        _that.msgpop.pop();

                                    }
                                )
                            }
                        )
                    },
                    'userId'
                )
            }

            this.sendmsg = function(data){
                data.recevier = JSON.stringify(_that.recevier);
                console.log(data);
                $.post(
                    '/user/sendmessage',
                    data,
                    function(){
                        _that.widget.msgbox(_that.lang=='true', 'info', '消息', '消息已经发送成功！');
                        _that.msgpop.close();
                    }
                )
            }

            this.init = function(){
                _that.container.find('#create').click(
                    _that.createUser
                )

                _that.container.find('#search').click(
                    _that.queryUser
                )

                _that.widget.autoComplete(
                    _that.container.find('#selecEmail'),
                    '/user/getByEmailAndPid',
                    "<ul>{{each list as data}}<li class='autoitem' data={{data.userId}}>{{data.email}} </li>{{/each}}</ul>",
                    function(data){ return data;},
                    function(){} ,
                    function(){

                    }
                );

                _that.widget.autoComplete(
                    _that.container.find('#selectName'),
                    '/user/getByNameAndPid',
                    "<ul>{{each list as data}}<li class='autoitem' data={{data.userId}}>{{data.username}} </li>{{/each}}</ul>",
                    function(data){ return data;},
                    function(){} ,
                    function(){
                    }
                );

                $.post(
                    '/jouranl/getAllRoleForJournal',
                    function(rst){
                        var data = {}; data.list = rst;
                        var tpl = "<ul>{{each list as data}}<li rid='{{data.id}}'>{{data.name}}</li>{{/each}}</ul>";
                        var render = template.compile(tpl);
                        var dom = render(data);

                        var roleList = _that.container.find('#selectRole .autolist');
                        roleList.append(dom).find('li').click(
                            function(){
                                _that.container.find('#selectRole .autoinput')
                                    .attr('rid', $(this).attr('rid'))
                                    .val($(this).text());

                            }
                        );

                        _that.container.find('#selectRole .autoinput').click(
                            function(e){
                                roleList.show();
                                e.stopPropagation();
                            }
                        );

                    }
                )

                $(body).click(
                    function(){
                        $('.autolist').hide();
                    }
                )

                this.container.find('#querynum span.nocheck').click(
                    function(){
                        $(this).parent().find('.nocheck').removeClass('checked');
                        $(this).addClass('checked');
                    }
                )

            }

            this.exports = {
            	'sendmsg' : this.sendmsg
            }

        }
        return UserManagement;
    }
)