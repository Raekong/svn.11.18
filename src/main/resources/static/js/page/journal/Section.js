define(
    ["widget", "popup", "template"],
    function( wd, popup, template ){
        function Section( container ){
            this.wd = new wd();
            this.container = container;
            var _that = this;

            this.lang = $('#frame').attr('i18n');

            this.setContainer = function(container){
                this.container = container;
            }

            this.load = function(){
                $.post(
                    '/journal/getAllSections',
                    function(rst){
                        var sections = JSON.parse( rst );
                        console.log( sections );
                        sections.forEach(
                            function(data){
                                if( data.authority ) data.authority = '已授权';
                                else data.authority = '未授权';

                                if( data.open ) data.status = '开放';
                                else data.status = '关闭';
                                if( data.expireDay > '2100') data.expireDay = '永久';
                                //console.log( data);
                            }
                        )
                        $('#slist').find('.body').empty();
                        new _that.wd.getTable()( $('#slist'), template, sections, function(rst){
                            rst.find('.enable').click(
                                function(){
                                    var id = $(this).parent().parent().attr('id');
                                    _that.setOpen( id,true)
                                }
                            )
                            rst.find('.ban').click(
                                function(){
                                    var id = $(this).parent().parent().attr('id');
                                    _that.setOpen( id,false)
                                }
                            )
                            rst.find('.up').click(
                                function(){
                                    _that.changeOrder($(this), 'up');
                                }
                            )
                            rst.find('.down').click(
                                function(){
                                    _that.changeOrder($(this), 'down');
                                }
                            )

                            rst.find('.config').click(
                                function(){
                                    var id = $(this).parent().parent().attr('id');
                                    _that.editSection(id);
                                }
                            )

                            rst.find('.skip').click(
                                _that.skip
                            )
                        }, 'id,journalId,order');
                    }
                )
            }

            this.editSection = function(id){
                $.post(
                    '/journal/section/getById',
                    {id: id},
                    function(rst){
                        _that.createSection(rst);
                    }
                )
            }

            this.skip = function(  ){
                var id = $(this).closest('.row').attr('id');
                
                $.post(
                    '/component/order-skip-pop/'+ (_that.i18n == 'true' ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.orderpop = new popup(title, dom,  ['568px'], function(dom, index){
                            dom.find('#cancel').click(
                                function(){
                                    _that.orderpop.close();
                                }
                            )

                            dom.find('#save').click(
                                function(){
                                    var neworder = dom.find('[data=order]').val();
                                    var rows = _that.container.find('#slist .body .row');
                                   
                                    if( !!neworder && neworder > 0 && neworder <= rows.length ){
                                        var domposition = neworder - 1;
                                        var preposition = domposition - 1;
                                        var setorder = -1;
                                        if( domposition <=0 ) {
                                            setorder = (0 + rows.eq(0).attr('order')*1.0)/2;
                                        }else{
                                            setorder = (rows.eq(preposition).attr('order')*1.0 + rows.eq(domposition).attr('order')*1.0)/2;
                                        }

                                        $.post(
                                            '/journal/section/order',
                                            {id: id, order: setorder},
                                            function( rst ){
                                                _that.load();
                                            }
                                        )
                                        
                                    };
                                    _that.orderpop.close();
                                }
                            )

                            dom.find('#cancel').click(
                                function(){
                                    _that.orderpop.close();
                                }
                            )
                        });
                        _that.orderpop.pop();            
                })
            }


            this.createSection = function( section ){
                $.post(
                    '/journal/create-section-pop',
                    function(rst){
                        var dom = $(rst);
                        var inputCells = dom.find("div[cell]");
                        inputCells.each(
                            function(){
                                _that.wd.init($(this), 'input');
                            }
                        );
                        
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  ['568px'], function(dom, index){
                            _that.wd.datepicker( '#sectionexpireday', 'cn');

                            //重载装入已有信息，如果没有，则不需要填充
                            
                            if( !!section ){
                                if( section.expireDay < '2100'){
                                    dom.find('#unexpired').removeClass('checked');
                                    dom.find('#noexpireday').hide();
                                    dom.find('#sectionexpireday').val( section.expireDay ).show();
                                }
                                dom.find('[data-title]').val(section.title);
                                if(section.authority) dom.find('#authority').addClass('checked'); 
                                dom.find('#editoremail').val(section.sectionEditor);
                                
                                dom.attr('sectionId', section.id);
                            }

                            _that.wd.autoComplete(
                                dom.find('#selecteditor'), 
                                '/user/getByEmailAndPid', 
                                "<ul>{{each list as data}}<li class='autoitem' data={{data.userId}}>{{data.email}} </li>{{/each}}</ul>", 
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

                            dom.find('#authority').click(
                                function(){
                                    if($(this).hasClass('checked')){
                                        $(this).removeClass('checked');
                                    }else{
                                        $(this).addClass('checked');
                                    }
                            });

                            dom.find('#unexpired').click(
                                function(){
                                    if($(this).hasClass('checked')){
                                        $(this).removeClass('checked');
                                        dom.find('#sectionexpireday').show();
                                        dom.find('#noexpireday').hide();
                                    }else{
                                        dom.find('#noexpireday').show();
                                        dom.find('#sectionexpireday').hide();
                                        $(this).addClass('checked');
                                    }
                            });

                            dom.find('#save').click(
                                function(){
                                    var title = dom.find('[data-title]').val().trim();
                                    var isAuthority = false;
                                    if( dom.find('#authority').hasClass('checked')) 
                                        isAuthority = true;
                                    var email = dom.find('#editoremail').val().trim();

                                    var expireDate = '2200-10-01';
                                    if( !dom.find('#unexpired').hasClass('checked') ) 
                                        expireDate = dom.find('#sectionexpireday').val();
                                    
                                    var sectionId =  dom.attr('sectionId');
                                    var data = {
                                        title : title,
                                        authority: isAuthority,
                                        email : email,
                                        expireDate : expireDate,
                                        id : !!sectionId ? sectionId : -1
                                    }

                                    $.post(
                                        '/journal/section/save',
                                        data,
                                        function(){
                                            _that.load();
                                            _that.pop.close();
                                        }
                                    )
                            });
            
                            dom.find("#cancel").click(
                                function(){
                                    _that.pop.close();
                            })

                        });
                        _that.pop.pop();
                    }
                )
            }

            this.setOpen = function( id, open ){
                $.post(
                    '/journal/section/open',
                    { id:id, open:open},
                    function(res){
                        _that.load();
                    }
                )
            }

            this.changeOrder = function( dom, dir ){
                var dom1 = null;
                var dom2 = null;
                var neworder = 0;
                
                if( dir == 'up'){
                    dom1 = dom.closest('.row').prev();
                }else{
                    dom1 = dom.closest('.row').next();
                }

                if( dom1.length > 0 ){
                    if( dir == 'up'){
                        dom2 = dom1.prev();
                    }else{
                        dom2 = dom1.next();
                    }

                    if( dom2.length > 0 ){
                        neworder = (dom1.attr('order')*1.0 + dom2.attr('order')*1.0)  / 2;
                    }else{
                        if(dir == 'up') neworder = dom1.attr('order') / 2;
                        else neworder = dom1.attr('order')*1.0 + 1;
                    }
                }
                if( neworder > 0){
                    var id = dom.closest('.row').attr('id');
                    $.post(
                        '/journal/section/order',
                        {id: id, order: neworder},
                        function( rst ){
                            _that.load();
                        }
                    )
                }
            }
        }
        return Section;
    }
)

