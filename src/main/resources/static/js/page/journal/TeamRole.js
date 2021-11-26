define(
    ["widget", "popup", "template"],
    function(wd, popup, template){
        function Team ( container ){
            this.container = container;

            var _that = this;
            this.wd = new wd();

            this.createRole = function(){
                $.post(
                    '/journal/create-role-pop',
                    function( rst ){
                        
                        var dom = $(rst);
                        var inputCells = dom.find("div[cell]");
                        inputCells.each(
                            function(){
                                _that.wd.init($(this), 'input');
                            }
                        )
                        var title = dom.find('.title').text();
                        _that.pop = new popup(
                            title, 
                            dom,  
                            ['568px'], 
                            function(dom, index){
                                dom.find("#save").click(
                                    function(){
                                        dom.find('.error').removeClass('error');
                                        var data = {};
                                        data.orgRoleId = dom.find('.autoinput').attr('data');
                                        data.abbr = dom.find('[data-abbr]').val();
                                        data.zh = dom.find('[data-zh]').val();
                                        data.en = dom.find('[data-en]').val();

                                        
                                        if( !data.orgRoleId ){
                                            dom.find('.autoinput').addClass('error');
                                            dom.find('.errorinfo').text('请选择与新建角色等同权限的系统角色');
                                            return; 
                                        }

                                        if( !data.zh && !data.en){
                                            dom.find('[data-zh], [data-en]').addClass('error');
                                            dom.find('.errorinfo').text('请至少填写角色中文名或英文名中的一个');
                                            return;
                                        }

                                        if( !data.abbr ){
                                            dom.find('[data-abbr]').addClass('error');
                                            dom.find('.errorinfo').text('请填写角色的英文缩写名');
                                            return;
                                        }

                                        console.log(data);
                                        $.post(
                                            '/journal/role/save',
                                            data,
                                            function(rst){
                                                console.log(rst);
                                                _that.init();
                                            }
                                        )
                                        _that.pop.close();
                                })

                                dom.find("#cancel").click(
                                    function(){
                                        _that.pop.close();
                                })
                                
                                _that.wd.autoComplete(
                                    dom.find('#sameroleassign'), 
                                    '/jouranl/getOriginRole', 
                                    "<ul>{{each list as data}}<li class='autoitem' data={{data.id}}>{{data.name}}</li>{{/each}}</ul>", 
                                    function(data){var rst = []; data.forEach(function(v){ if(v.id!=1) rst.push(v); }); return rst;}, //不允许创建和MANAGER一样的角色同名
                                    function(){} ,
                                    function(){}               
                                );

                        });
                        _that.pop.pop();
                    }
                )
            }

            this.roleAssign = function(){
                $.post(
                    '/journal/role-assign-pop',
                    function( rst ){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.roleAssignPop = new popup(
                            title, 
                            dom,  
                            '568px', 
                            function(dom, index){
                                _that.wd.autoComplete(
                                    dom.find('#user'), 
                                    '/user/getByEmailAndPid', 
                                    "<ul>{{each list as data}}<li class='autoitem' data={{data.userId}}>{{data.email}}</li>{{/each}}</ul>", 
                                    function(data){  
                                        var rst = [];
                                        data.forEach(
                                            function(d){
                                                if(!d.disabled){
                                                    rst.push(d);
                                                }
                                            }
                                        )
                                        return rst;
                                    },
                                    function(){},
                                    function(){}             
                                );

                                _that.wd.autoComplete(
                                    dom.find('#role'), 
                                    '/jouranl/findRoleByJournalId', 
                                    "<ul>{{each list as data}}<li class='autoitem' data={{data.id}}>{{data.name}}</li>{{/each}}</ul>", 
                                    function(data){return data}, 
                                    function(){},
                                    function(){}             
                                );

                                
                                dom.find("#save").click(
                                    function(){
                                        var data = {};
                                        data.uid = dom.find('#userid').attr('data');
                                        data.rid = dom.find('#roleid').attr('data');
                                        if( !data.uid || !data.rid){
                                            dom.find('.errorinfo').text('请指定赋予角色及对应的用户');
                                            return;
                                        }
                                        //journal manager只能由一名用户承担
                                        if(data.rid == 1){
                                            var total = $('[rid=1]').attr('total')*1.0;
                                            if( total > 0 ){
                                                dom.find('[data-abbr]').addClass('error');
                                                dom.find('.errorinfo').text('Journal Manager只有由一名用户承担');
                                                return;
                                            }
                                        }

                                        $.post(
                                            '/jouranl/saveUserRoleRelation',
                                            data,
                                            function( rst ){
                                                if( !rst ){
                                                    _that.wd.msgbox(
                                                        _that.lang =='true',
                                                        'alert',
                                                        '消息',
                                                        '为同一个用户重复设置相同的角色'
                                                    )
                                                    return; //同一个人重复设置角色
                                                }
                                                _that.init();
                                                _that.roleAssignPop.close();
                                            }
                                        )
                                        
                                })


                                dom.find("#cancel").click(
                                    function(){
                                        _that.roleAssignPop.close();
                                })
                        })
                        _that.roleAssignPop.pop();
                    }
                )
            }

            this.init = function( ){
                
                this.container.find('#createRole').unbind().click(
                    this.createRole
                );

                this.container.find('#roleAssign').unbind().click(
                    this.roleAssign
                );
                
                this.getTeam( this.container );
            }

            this.removeMember = function(){
                var urrid = $(this).closest('.row').attr('urrid');
                _that.wd.msgbox(_that.lang == 'true', 'alert', "警告", "确定从当前角色中移除该用户?", function(){
                    $.post(
                        '/journal/team/remove',
                        {urrid: urrid},
                        function( layer ){
                            _that.init();   
                            _that.teamPop.close();
                        }
                    )
                });
            }

            this.configTeam = function( dom ){
                var rid = $(this).closest('.row').attr('rid');
                $.post(
                    '/journal/team-config-pop',
                    function( rst ){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.teamPop = new popup(
                            title, 
                            dom,  
                            '868px', 
                            function(dom, index){
                                $.post(
                                    '/journal/getTeamUserByRole',
                                    {rid : rid},
                                    function(rst){
                                        rst.forEach(
                                            function( r ){
                                                console.log(r);
                                                if( !r.name ) r.name = '-';
                                            }
                                        )
                                        new _that.wd.getTable()( dom.find('#userList'), template, rst, function(dom){
                                            dom.find('.remove').click(
                                                _that.removeMember
                                            );
                                        }, 'urrid');
                                    }
                                );
                                dom.find("#cancel").click(
                                    function(){
                                        _that.teamPop.close();
                                })
                        })
                        _that.teamPop.pop();
                    }
                );
            }

            this.getTeam = function( dom ){
                $.post(
                    '/journal/getTeamStatis',
                    function( rst ) {
                        var teams = JSON.parse( rst );
                        dom.find('#teamlist').find('.body').empty();
                        new _that.wd.getTable()( dom.find('#teamlist'), template, teams, function(dom){
                            dom.find('.config').click(
                                _that.configTeam
                            );
                        }, 'rid,total,samelevel');
                    }
                )
            }
    
        }

        return Team;
    }
    
)