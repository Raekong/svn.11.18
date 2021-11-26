define(
    ["homecommon", "template", "widget" , "popup",  "editor", "eventcenter", "uploader", "submitdc", "submitchecker"],
    function(hc, template, wd, popup,  editor, ec, uploader, dc, checker){
        function Home( container){
            this.name = 'SUMBIT';

            this.container = container;
            this.lang = container.attr('i18n');

            this.ec = new ec('debug');
            this.ec.addHandler( this );

            this.dc = new dc(this.ec);
            this.checker = new checker(this.ec);

            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li').removeClass('current');
                }
            );
            this.wd = new wd();

            var _that = this;

            this.init = function(){
                this.ed1 = new editor('abstract', this.lang=='true', 208);
                this.ed1.init();
                //确认承诺
                this.container.find('#accept').click(
                    function(){
                        if( $(this).hasClass('checked')){
                            $(this).removeClass('checked');
                            _that.ec.fire(
                                'sumbit',
                                'accept',
                                false
                            )
                        }else{
                            $(this).addClass('checked');
                            _that.ec.fire(
                                'sumbit',
                                'accept',
                                true
                            )
                        }
                    }
                )

                this.container.find("#mastheadmask").click(
                    function(){
                        _that.wd.msgbox(
                            _that.i18n == 'true',
                            'alert',
                            'Message',
                            'Please upload Manuscript in word or Manuscript in pdf files first.',
                            function(){
                               
                            }
                        )
                    }
                )

                //初始化SECTION
                $.post(
                    '/journal/getSectionByTitleLike',
                    {title:''},
                    function(data){
                        data = JSON.parse(data);
                        var rst = [];
                        data.forEach( function( d ){
                            if(d.open){
                                rst.push(d);
                            }
                        })

                        rst.sort(function(a, b){return a.order - b.order});

                        var data = {}; data.list = rst;
                        var tpl = "<ul>{{each list as data}}<li class='autoitem' data={{data.id}}>{{data.title}} </li>{{/each}}</ul>";
                        var dom  = template.compile(tpl)(data);
                       
                        _that.container.find('#sectionslist .autolist').append(dom);
                        _that.container.find('#sectionslist .autolist li').click(
                            function(){
                                _that.container.find('#sectionslist input').val($(this).text());
                                _that.ec.fire(
                                    'submit',
                                    'sectionSelect',
                                    { id: $(this).attr('data'), text: $(this).text()}
                                )
                            }
                        );
                    }
                )

                this.container.find('#sectionslist input').click(
                    function(e){
                        _that.container.find('#sectionslist .autolist').show();
                        e.stopPropagation();
                    }
                )
                
                //上传材料
                this.container.find('#upload').click(
                    _that.uploadFile
                )

                //添加作者
                this.container.find('#addAuthor').click(
                    _that.addAuthor
                )

                
                //添加审稿人
                this.container.find('#addReviewer').click(
                    _that.addReviewer
                )


                //暂存数据在缓存
                this.container.find('#savecache').click(
                    function(){
                        var data = {};
                        data.title = _that.container.find('[data-title]').val();
                        data.abstract = _that.ed1.html();
                        data.keywords =  _that.container.find('[data-keywords]').val();
                        _that.ec.fire(
                            'submit'
                            ,'savecache'
                            ,data
                        )
                        _that.wd.msgbox(
                            _that.lang == 'true',
                            'alert',
                            '消息',
                            '论文的信息已经缓存，请尽快完成论文提交'
                        );
                        return;
                    }
                )

                this.container.find('#submit').click(
                    function(){
                        
                        _that.container.find('.error').removeClass('error');
                        var data = {};
                        data.title = _that.container.find('[data-title]').val();
                        data.abstract = _that.ed1.html();
                        data.keywords =  _that.container.find('[data-keywords]').val();

                        _that.ec.fire(
                            'submit',
                            'finalsubmit',
                            data
                        )
                    }
                )


                _that.container.find('#closePrompt').click(
                    function(){
                        $('#cachemetion').hide();
                    }
                )

                _that.container.find('#clearCache').click(
                    function(){
                        _that.ec.fire(
                            'submit',
                            'clearCache',
                            {}
                        )
                    }
                )

                $('body').click(
                    function(){
                        $('.autolist').hide();
                    }
                )
                
                //=============缓存＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
                var cache = this.ec.fire(
                    'submit',
                    'loadCache',
                    _that.ed1
                )
                if( !!cache[0].rst ){
                    _that.container.find('#mastheadmask').remove();
                    $('#cachemetion').show();
                }
            }

            //===========analysis Paper=========================================================================
            this.removeMastheadMask = function(file){
                //如果已经移除了就不要再移，和分析论文了
                if( _that.container.find('#mastheadmask').length == 0) return;
                $('#loadinggif').show();
                $.post(
                    '/submit/analysisPaper',
                    file,
                    function( rst ){
                        console.log(rst);
                        $('#loadinggif').hide();
                        _that.ed1.set(rst.article_abstract);
                        _that.container.find('[data-title]').val(rst.article_title);
                        _that.container.find('[data-keywords]').val(rst.article_keywords);
                        _that.container.find('#mastheadmask').remove();

                        _that.ec.fire(
                            'submit',
                            'analysAuthor',
                            rst.author_name
                        )
                    }
                )

            };            

            //===========add reviewer=========================================================================
            this.addReviewer = function(){
                $.post(
                    '/submit/submit-reviewer-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '888px', function(dom, index){
                            var inputCells = dom.find("#form [cell]");
                            inputCells.each(
                                function(){
                                    _that.wd.init($(this), 'input');
                                }
                            )

                            _that.ec.fire(
                                'submit',
                                'newReviewer',
                                null
                            )

                            dom.find('#addConfirm').click(
                                function(){
                                    _that.ec.fire(
                                        'submit',
                                        'addReviwer',
                                        dom
                                    );

                                    dom.find('#save').show();
                                }
                            )

                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            );

                            dom.find('#save').click(
                                function(){
                                    _that.ec.fire(
                                        'submit',
                                        'reviewerAddEnd',
                                        null
                                    )
                                    _that.pop.close();
                                }
                            )

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
                                function( item ){
                                    var id = item.attr('data');
                                    _that.fillUser(id, dom);
                                }             
                            );

                        })
                        _that.pop.pop();    
                    }
                )
            }

            //===========add Author=========================================================================
            this.addAuthor = function( data ){
                _that.ec.fire(
                    'submit',
                    'addNewAuthor',
                    null
                )
            }

            this.fillUser = function(id, dom){
                $.post(
                    '/user/findById',
                    {id:id},
                    function(rst){
                        console.log(rst);
                        dom.find('[data-name]').val( rst.username );  
                        dom.find('[data-affiliation]').val( rst.affiliation );  
                        dom.find('[data-researchfield]').val( rst.interests );  
                    }
                )
            }
            

            //===========upload file=========================================================================
            this.uploadFile = function(){
                $.post(
                    '/submit/submit-upload-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '788px', function(dom, index){
                            var uploader = _that.initUploader(dom.find('#uploadDiv'), _that.ec, null, (this.lang== 'true'));
                            uploader.init();
                            
                            _that.ec.fire(
                                'uploadFile',
                                'newUpload',
                                null
                            )

                            dom.find('#uploadConfirm').click(
                                function(){
                                    _that.getFileInfo(uploader, dom) ;     
                                }
                            )

                            dom.find('#fileType').click(
                                function(e){
                                    dom.find('.autolist').show();
                                    e.stopPropagation();    
                                }
                            )

                            dom.find('.autolist ul li').click(
                                function(e){
                                    dom.find('#fileType').val($(this).text());
                                    dom.find('.autolist').hide();
                                }
                            )
                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )

                           
                            dom.find('#save').click(
                                function(){
                                    _that.ec.fire(
                                        'submit',
                                        'fileUploadEnd',
                                        null
                                    )
                                    _that.pop.close();
                                }
                            )
                            
                        });
                        _that.pop.pop();
                    }
                )
            }
            
            var _currentFile = {};
            this.getFileInfo = function(up, dom){
                $('#fileType').removeClass('error');

                if( !$('#fileType').val()){
                    $('#fileType').addClass('error');
                    return;
                }

                _currentFile.filetype = $('#fileType').val();
                up.upInterface.upload();
                       
            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    ec,
                    null,  //确定上传按键
                    lang
                );
                return up;
            }

            this.uploaded = function(data){
                if( data == null ){
                    $('.note').text('上传文件类型错误，或文件大小超过限定值');
                    return ;
                }
                console.log(_currentFile);
                var file = $.extend(_currentFile, data); 
                console.log(_currentFile);
                _currentFile = {}
                _that.ec.fire(
                    'SUMBIT',
                    'fileuploaded',
                    file
                );
                $('#fileType').val('');
                $('.note').text('');
                $('.pre').css('width', '0px');
                $('#save').show(); 
            }

            this.uploadFileRepeat = function(){
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'alert',
                    '消息',
                    'A file of the same type has been uploaded. Please confirm the type of uploaded file.'
                )
            }
            //=========== submit =========================================================================
            this.checkerror = function(info){
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'alert',
                    '消息',
                    info
                )
            }

            this.sumitVerifyFailed = function( data){
                
                _that.container.find('[data='+data+']').addClass("error");
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'alert',
                    '消息',
                    '请注意完善作者信息，并保证作者姓名及邮箱填写正确'
                )

            }

            this.submitDone = function(){
                $.post(
                    '/submit/submit-confirm-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '568px', function(dom, index){
                            dom.find('#cancel').click(
                                function(){
                                    _that.ec.fire(
                                        'submit',
                                        'clearCache',
                                        null
                                    )
                                    window.location.reload();
                                    _that.pop.close();
                                }
                            )
                        })
                        _that.pop.pop();
                    }
                )
            }

            this.exports = {
                'uploaded' : this.uploaded,
                'uploadFileRepeat': this.uploadFileRepeat,
                'modifyUser' : this.addAuthor,
                'checkerror' : this.checkerror,
                'submitdone' : this.submitDone,
                'removeMastheadMask' : this.removeMastheadMask,
                'authorVerifyFailed' : this.sumitVerifyFailed
            }

            
        }
        return Home;
    }
)