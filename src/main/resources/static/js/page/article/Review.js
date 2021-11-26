define(
    ["editor", "uploader", "eventcenter", "template", "store", "widget"],
    function( editor , uploader, eventcenter, tpl, store, wd ){
        function Review(container, ec){
            this.name = "REVIEW";
            this.container = container;
            this.ec = !!ec ? ec : new eventcenter('debug');
            this.ec.addHandler(this);

            this.ed1 = null;
            this.ed2 = null;

            this.lang = $('#reviewpage').attr('i18n');
            this.wd = new wd();

            var _that = this;

            this.init = function(){
                

                this.container.find("#decline,#accept").click(
                    _that.requestDo
                );

                if($('#reviewfiles').length > 0){
                    var isView = (this.container.attr('viewresult') == 'true');
                    
                    this.ed1 = new editor('author', this.lang=='true', 208);
                    this.ed1.init();

                    this.ed2 = new editor('editor', this.lang=='true', 208);
                    this.ed2.init();

                    this.container.find('#files .row span.nocheck').click(
                        function(){
                            if($(this).hasClass('checked')){
                                $(this).removeClass('checked')
                            }else{
                                $(this).addClass('checked')
                            }
                        }
                    )

                    this.container.find("#downloadZip").click(
                        function(){
                            var selected = $('#files .body span.checked');
                            if( selected.length == 0 ) return;
                            var tmp = [];
                            for(var i=0; i<selected.length; i++){
                                tmp.push(selected.eq(i).closest('.row').attr('data'));
                            }
                            var fileStr = tmp.join(";");
                            console.log(fileStr);
                            window.open("/article/file/download?appends="+fileStr);
                        }
                    )
    
                    this.container.find('#selectall').click(
                        function(){
                            _that.container.find('#files .body span.nocheck').removeClass('checked').addClass('checked');
                        }
                    )
    
                    this.container.find('.cell.file').click(
                        function(){
                            var link = $(this).attr('href');
                            var fileName = $(this).text();
                            _that.download(link, fileName);
                        }
                    )
                    //============此上查看，与正常审稿均可－－－－－－－－－－－－－－－－－－－－－－－－


                    if( isView ){
                        _that.container.find('#selecteditor').remove(); //移除文件上传组件
                        _that.container.find('#submit').remove(); //移除文件上传组件
                        _that.container.find('#savecache').remove(); //移除文件上传组件
                        _that.container.find('#savecache').remove(); //移除文件上传组件.finalFiles
                        var raid = _that.container.attr('raid')
                        _that.container.find('#finalFiles').empty();
                        $.post(
                            '/review/result/check',
                            {raid:raid},
                            function(rst){
                                console.log(rst);
                                _that.ed1.set(rst.commendforAuthor);
                                _that.ed2.set(rst.commendforEditor);
                                _that.container.find('.recommend span[type="'+ rst.recommendType +'"] span.nocheck').addClass('checked'); //移除文件上传组件
                                _that.attachments = JSON.parse(rst.filesStr);

                                _that.appendReviewFile( _that.attachments );
                            }
                        )
                        return;
                    }
                    var uploader = this.initUploader(_that.container.find('#uploadDiv'), _that.ec, _that.container.find('#uploadConfirm') , (this.lang== 'true'));
                    uploader.init();


                    this.container.find('span.recommend').click(
                        function(){
                            var type = $(this).attr('type');
                            $(this).closest('.emp').find('span.nocheck').removeClass('checked');
                            $(this).find('span.nocheck').addClass('checked');
                            _that.recommend = type;
                        }
                    )

                    this.container.find('#submit').click(
                        function(){
                            var data = _that.getherinfo();
                            data.files = JSON.stringify(data.files);
                            data.raid = $('#reviewpage').attr('raid');
                            if( !data.author || !data.recommend ){
                                _that.wd.msgbox(
                                    _that.lang == 'true',
                                    'info',
                                    'Submit Review Recommend',
                                    'Please fill the Comment for Authors & Editors and give Review Recommendation'
                                )
                            }
                            //console.log(data);
                            $.post(
                                '/review/submit',
                                data,
                                function(){
                                    _that.wd.msgbox(
                                        _that.lang == 'true',
                                        'info',
                                        'Submit',
                                        'Thank you for your cooperation, the review results have been submitted successfully ',
                                        function(){
                                            store.set('review_rst', null);
                                            window.location.reload();
                                        }
                                    )
                                }
                            )
                        }
                    )

                    this.container.find('#savecache').click(
                        function(){
                            var data = _that.getherinfo();
                            store.set('review_rst', data);
                            _that.wd.msgbox(
                                _that.lang == 'true',
                                'info',
                                'Save Cache',
                                'Review data has saved successful!'
                            )
                        }
                    )

                    var cache = store.get('review_rst');
                    if( !!cache ){
                        _that.fillCache(cache);
                        _that.container.find('#cachemetion').show();
                        _that.container.find('#closePrompt').click(
                            function(){
                                _that.container.find('#cachemetion').hide();
                            }
                        )
                        _that.container.find('#clearCache').click(
                            function(){
                                store.set('review_rst', null);
                                window.location.reload();
                            }
                        )
                    }
                }
            }

            this.appendReviewFile = function( files ){
                console.log(files);
                var dom = '<div class="tablehead">' +
                          '<div  class="cell" data="originName">File Name</div>' +
                          '</div>';
                var tpls = '{{each files as file }}<div class="row"><div class="cell file" style="flex:1 1 100%" innerId="{{file.innerId}}" link="{{file.url}}">{{file.originName}}</div></div>{{/each}}'
                var data = {}; data.files = files;
                dom += tpl.compile(tpls)(data);
                _that.container.find('#finalFiles').append(dom).find('[link]').click(
                    function(){
                        var link = $(this).attr('href');
                        var fileName = $(this).text();
                        _that.download(link, fileName);
                    }
                );
            }

            this.fillCache = function( cache ){
                _that.attachments = cache.files;
                _that.renderFile();
                _that.ed1.set( !!cache.author?cache.author:'');
                _that.ed2.set( !!cache.editor?cache.editor:'');

                if(!!cache.recommend){
                    _that.recommend = cache.recommend;
                    _that.container.find('span[type="'+cache.recommend+'"] .nocheck').addClass('checked');
                }
            }

            this.getherinfo = function(){
                var data = {
                    jid : $('#reviewpage').attr('jid'),
                    aid : $('#reviewpage').attr('aid'),
                    files: _that.attachments, 
                    recommend:_that.recommend, 
                    editor: _that.ed2.txt(), 
                    author:_that.ed1.txt()  
                };
                return data;
            }

            this.download = function(url, filename){
                var xhr = new XMLHttpRequest();
                xhr.open('GET', url, true);
                xhr.responseType = 'blob';
                xhr.onload = function () {
                    var blob = xhr.response;
                    if (xhr.status === 200) {
                        if (window.navigator.msSaveOrOpenBlob) {
                            navigator.msSaveBlob(blob, filename);
                        }else {
                            var link = document.createElement('a');
                            var body = document.querySelector('body');
                            link.href = window.URL.createObjectURL(blob);
                            link.download = filename;
                            // fix Firefox
                            link.style.display = 'none';
                            body.appendChild(link);
                            link.click();
                            body.removeChild(link);
                            window.URL.revokeObjectURL(link.href);
                        };
                    }
                };
                xhr.send();
            }            

            this.requestDo = function(  ){
                var raid = $('#reviewpage').attr('raid');
                var type = $(this).attr('id');
                $.post(
                    '/review/'+ type + "/" +raid,
                    function(){
                        console.log(window.location);
                        window.location.reload();
                    }
                )
            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    ec,
                    uploadBut,  //确定上传按键
                    lang
                );
                return up;
            }

            this.attachments = [];
            this.fileTpl = '{{ each list as file}}<div class="row"><div   style="flex: 1 1 90%"  data="{{file.innerId}}">{{file.originName}}</div><div  style="flex: 1 1 10%"><span class="del"><i class="fa fa-close"></i></span></div></div>{{/each}}'
            this.renderFile = function(){
                

                var data = {}; data.list = _that.attachments;
                var dom = tpl.compile(_that.fileTpl)(data);
                _that.container.find('#finalFiles .body').empty().append( dom );
                _that.container.find('#finalFiles .body .row span.del').click(
                    _that.removeFile
                )
            }
            this.removeFile = function(){
                var innerId = $(this).closest('.row').attr('data');
                var index = 0;
                for(; index< _that.attachments.length; i++){
                    _that.attachments[index].innerId = innerId;
                    break;
                }
                _that.attachments.splice(index, 1);
                $(this).closest('.row').remove();
                console.log(_that.attachments);
            }

            this.uploaded = function(data){
               
                if( data == null ){
                    $('.note').text('文件大小超过限定值');
                    return ;
                }
                _that.attachments.push( data );
                _that.renderFile();
            }

            this.exports = {
                "uploaded": this.uploaded
            }
        }

        return Review;
    }
)