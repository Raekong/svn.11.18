define(
    ["homecommon", "widget", "editor", "datacenter", "template",  
     "configds", "team", "section", "email", "submitreview", "workflow", "payment"],
    function( hc, widget, ed, dc, tpl,  ds, team, sc, email, submitreview, workflow, payment){
        function Config(container ){
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#5').addClass('current');
                }
            );

            this.i18n = JSON.parse(this.container.find('[i18n]').attr('i18n'));
            this.lang = $('#frame').attr('i18n');
            
            var isZh = $('#frame').attr('i18n') == 'true';
            this.ed1 = new ed('editor1-container-atj', isZh, 208);
            this.ed2 = new ed('editor2-container-iaa', isZh, 208);
            this.ed3 = new ed('editor3-container-ci', isZh, 208);
            this.eds = []

            this.widget = new widget();
            this.widget.init( this.container.find('#tabs'), 'tab');
            
            var _that = this;
            this.init = function(){
                var sections = ["masthead","contact"]
                for(var section of sections){
                    _that.Cells(section)
                }

                _that.ed1.init();
                _that.ed2.init();
                _that.ed3.init();
                _that.eds.push(_that.ed1);
                _that.eds.push(_that.ed2);
                _that.eds.push(_that.ed3);

                _that.container.find('#lang span.nocheck').click(
                    function(){
                        var lang = $(this).attr('lang');
                        $('#lang').attr('data-i18n', lang);
                        $('#lang span.nocheck').removeClass('checked');
                        $(this).addClass('checked');
                    }
                )

                _that.container.find('#submit-masthead, #submit-contact').click(_that.submit);

                var config_list = ["masthead","contact","section"]
                for(var config_point of config_list){
                    var params = { journalId:"1", configPoint:config_point };
                    $.post(
                        '/journal/getSetting',
                        params,
                        function (res){
                            if(res.configContent != undefined ){
                                var config = JSON.parse(res.configContent);
                                switch(res.configPoint){
                                    case "masthead":
                                    case "contact":
                                        _that.fill(res.configPoint, config);
                                        break;
                                    case "section":
                                        new _that.widget.getTable()(_that.container.find('.slist'), tpl, config, function(){
                                            console.log("getTable")
                                        }, "guid");
                                        break;
                                }
                            }
                        }
                    )
                }

                _that.section = new sc($('#section'));
                _that.section.load();
                _that.container.find('#createSection').click( _that.section.createSection );
                _that.widget.autoComplete(
                    _that.container.find('#querysection'), 
                    '/journal/getSectionByTitleLike', 
                    "<ul>{{each list as data}}<li class='autoitem' data={{data.id}}>{{data.title}} </li>{{/each}}</ul>", 
                    function(data){ return data; },
                    function(){},
                    function( dom ){
                        var id = dom.attr('data');
                        _that.container.find('#slist .row').removeClass('emphsis');
                        _that.container.find('#slist .row[id='+ id +']').addClass('emphsis');
                    }                
                );

                
                new team($('#team')).init();

                new email($('#emailconfig')).init();

                new submitreview($('#submitreview')).init();

                new workflow($('#workflow')).init();

                new payment($('#payment')).init();
            }

            this.Cells = function(section){
                var inputCells = _that.container.find("#"+section).find("div[cell]");
                inputCells.each(
                    function(){
                        _that.widget.init($(this), 'input');
                    }
                )
            }

            this.fill = function(configPoint, config){
                
                for(var attr in config){
                    var tag = '[data-'+attr+']';
                    if(tag=="[data-i18n]" ){
                        _that.container.find('[lang="'+config[attr]+'"]').click();
                        continue;
                    }
                    if(_that.container.find('#'+configPoint).find(tag).length>0){
                        _that.container.find('#'+configPoint).find(tag).val(config[attr])
                    }else{
                        for (var ed of _that.eds){
                            if(ed.domId.split('-').reverse()[0] === attr)
                                ed.set(config[attr])
                        }
                    }
                }
            }

            this.submit = function(){
                _that.container.find('.error').removeClass('error');
                var data={};
                var tag = container.find('a.active').attr('tag');
                switch (tag){
                    case 'masthead':
                        data = _that.masthead_dc.exec();
                        data.data.atj = _that.ed1.txt();
                        data.data.iaa = _that.ed2.txt(); 
                        break;
                    case 'contact':
                        data = _that.contact_dc.exec();
                        data.data.ci = _that.ed3.txt(); 
                        break;

                }
                if(!!data.verify.key){
                    data.verify.dom.addClass('error');
                    if(data.verify.key.indexOf('email') != -1){
                        _that.widget.msgbox(
                            _that.lang == 'true',
                            'alert',
                            _that.i18n.info, 
                            _that.i18n.emailfail
                        );
                    };
                }else{
                    console.log(data.data);
                    var params = {}
                    params.configPoint = tag
                    params.configContent = JSON.stringify(data.data)
                    $.post(
                        '/journal/save',
                        params,
                        function(res){
                            _that.widget.msgbox(
                                _that.lang == 'true',
                                'info',
                                _that.i18n.info, 
                                _that.i18n.configsus
                            );
                        }
                    )
                }
            }

            

            this.masthead_dc = new dc( container.find('#masthead'), ds.masthead_ds );
            this.contact_dc = new dc( container.find('#contact'), ds.contact_ds );
            this.section_dc = new dc( container.find('#section'), ds.section_ds );
        }
        return Config;
    }
)