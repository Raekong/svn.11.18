define(
    ["widget", "template", "datacenter", "mailtpl"],
    function(widget,tpl, dc, mailTpl){
        function Reviewer(ec, dom  ){
            this.name = "reviewer_invite";
            this.dom = dom;
            this.widget = new widget();
            this.ec = ec;
            this.ec.addHandler(this);
            this.lang = $('#frame').attr('i18n');
            this.rid = this.dom.find('#save').attr('rid');

            var _that = this;
            this.reviewers = [];
            this.ds = {
                
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },  'data-name': {
                    'verify': 'nonull',
                    'val' : ''
                },
                
                'data-affiliation': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-research': {
                    'verify': 'nonull',
                    'val' : ''
                }
            }

            this.init = function(){
                var inputCells = _that.dom.find("#queryboard div[cell]");
                inputCells.each(
                    function(){
                        _that.widget.init($(this), 'input');
                    }
                )

                this.existViewers = [];
                $('[rid='+_that.rid+'] .reviewaction div[email]').each(
                    function(){
                        _that.existViewers.push( $(this).text());
                    }
                )
               

                this.dom.find('input').keyup(
                    this.queryReviewers
                )

                this.dom.find('#cancel').click(
                    function(){
                        _that.ec.fire(
                            'reviewer',
                            'returnreviewer',
                            null
                        )
                    }
                )

                this.dom.find('#reviewselected').click(
                    function(){
                        _that.dom.find('#reviewselectcontr').hide();
                        _that.dom.find('#emailpart').show();
                    }
                )

                this.dom.find('#save').click(
                    function(){

                        var data = {};
                        data.reviewers = _that.reviewers;

                        if(data.reviewers.length == 0){
                            _that.ec.fire(
                                'reviewer',
                                'nullreviewer',
                                null
                            )
                            return;
                        }
                        data.attaches = [];
                        _that.dom.find('#files .row').each(
                            function(){
                                var file = {};
                                file.id = $(this).attr('id');
                                file.fileType =  $(this).find('[filetype]').attr('filetype');
                                file.originName = $(this).attr('data').split(',')[0];
                                file.innerId = $(this).attr('data').split(',')[1];
                                data.attaches.push(file)
                            }
                        )

                        data.title = _that.mail.getTitle();
                        data.content = _that.mail.getContent();
                        
                        $.post(
                            '/article/review/sendReviewRequest',    
                            {   data: JSON.stringify(data), jid: $('#frame').attr('jid'), 
                                aid: $('#frame').attr('aid'), rid: $(this).closest('[rid]').attr('rid')  },
                            function(){
                                _that.dom.find('#cancel').click();
                                window.location.reload();
                            }
                        )
                    }
                )

                this.dom.find('#clearReviewer').click(
                    function(){
                        _that.dom.find('#review div[cell] input').val('');
                    }
                )

                this.dom.find('#addreviewer').click(
                    function(){
                       _that.dom.find('.error').removeClass('error');
                       var d = new dc();
                       var data = {};
                        var txts = _that.dom.find('#review #queryboard div[cell]');
                        
                        txts.each(
                            function(){
                                data[$(this).attr('data')] = $(this).find('input').val().trim();
                            }
                        );
                        //console.log(data);
                        var rst = d.execData(_that.ds, data);
                        if( !!rst ){
                            console.log(rst);
                            _that.dom.find('input['+ rst +']').addClass('error');
                        }else{
                            _that.dom.find('#review div[cell] input').removeClass('error').val('');
                            _that.addReviewers(data);
                        }
                    }
                )


                this.dom.find('#suggestreviewer .row, #lastreviewer .row').click(
                    function(){
                        var data = {};
                        var txts = $(this).find('[data]');
                        
                        txts.each(
                            function(){
                                data[$(this).attr('data')] = $(this).text().trim();
                            }
                        );
                        _that.addReviewers(data);
                    }
                )

                this.reviewers = [];

                this.dom.find('#save').click(
                    function(){
                        _that.ec.fire(
                            'reviewer',
                            'selectReviewer',
                            _that.reviewers
                        )
                    }
                )

                //======email===========================
                $.post(
                    '/article/email/' ,
                    { 
                        aid:$('#frame').attr('aid'),  
                        jid: $('#frame').attr('jid'), 
                        configPoint: 'Review Invite', 
                        'i18n': (_that.lang=='true' ? 'zh' : 'en') 
                    },
                    function( rst ){
                        var tplDom = $(rst);
                        _that.dom.find('#emailTpl').append(tplDom);
                        _that.mail = new mailTpl( _that.dom.find('#emailTpl'), _that.lang=='true').init();
                        
                    }
                )

                this.dom.find('#files .del').click(
                    function(){
                        $(this).closest('.row').remove();
                    }
                )
            }

            this.selectedTpl = '<div class="row" email={{email}}>'
                             +'<div style="flex: 1 1 20%;" data=email>{{email}}</div>'
                             +'<div style="flex: 1 1 20%;" data=name>{{name}}</div>'
                             +'<div style="flex: 1 1 15%;" data=statis>{{!!statis ? statis : "-/-" }}</div>'
                             +'<div style="flex: 1 1 41%; position:relative;" data=dues><input class="due" style="margin-right:6px;" value={{responseDue}} data="responseDue"/><input class="due" value={{reviewDue}} data="reviewDue"/></div>'
                             +'<div style="flex: 1 1 4%;"><span class="del" style="cursor:pointer"><i class="fa fa-close"></span></div>'
                             +'</div>';

            this.addReviewers = function(data){
                var flag = false;
                _that.reviewers.forEach(
                    function(d){
                        if( data.email == d.email ){
                            _that.widget.msgbox(
                                _that.lang == 'true',
                                'alert',
                                '添加审稿人',
                                '不能重复添加审稿人！'
                            )
                            flag = true;
                            return;
                        }
                    }
                )

                _that.existViewers.forEach(
                    function(d){
                        if( data.email == d ){
                            _that.widget.msgbox(
                                _that.lang == 'true',
                                'alert',
                                '添加审稿人',
                                '添加的审稿人已经在审稿人列表中，不能重复添加！'
                            )
                            flag = true;
                            return;
                        }
                    }
                )

                if( flag ) return;


                data.responseDue = $('#reviewassign').attr('responseDue');
                data.reviewDue = $('#reviewassign').attr('reviewDue');
                var dom = tpl.compile(_that.selectedTpl)(data);;
                _that.dom.find('#selectedReviewer .body').append(dom);
                _that.dom.find('#selectedReviewer .body span.del').unbind().click(
                    function(){
                        _that.removeReviewers($(this).closest('.row').attr('email'));
                        $(this).closest('.row').remove();
                    }
                )

                _that.dom.find('#selectedReviewer .body input').each(
                    function(){
                        var dom = $(this);
                        _that.widget.datepicker(this, this.lang=='true' ? 'cn' : 'en', function(value, date, endDate){
                            var email = dom.closest('.row').attr('email');
                            var data = dom.attr('data');
                            _that.updateReviewer(data, email, value);
                        });
                    }
                )
                _that.reviewers.push(data);
            }

            this.updateReviewer = function(data, email, value){
                //console.log(_that.reviewers);
                var index = 0;
                for(; index<_that.reviewers.length; index++){
                    if(_that.reviewers[index].email == email){
                        break;
                    }
                }
                _that.reviewers[index][data] = value;
            }

            this.removeReviewers = function( email ){
                var index = 0;
                for(; index<_that.reviewers.length; index++){
                    if(_that.reviewers[index].email == email){
                        break;
                    }
                }

                _that.reviewers.splice(index, 1);
                //console.log(index);
            }


            this.queryReviewers = function(){
                var query = {};
                query.name = _that.dom.find("[data-name]").val();
                query.email = _that.dom.find("[data-email]").val();
                query.affiliation = _that.dom.find("[data-affiliation]").val();
                query.research = _that.dom.find("[data-research]").val();

                var flag = false;
                for(var key in query){
                    if(!!query[key]) flag = true;
                }

                if(!flag) return;

                _that.widget.getPage()(
                    _that.lang=='true' ? 'zh' : 'en',
                    tpl,
                    _that.dom.find('#queryRstbody'),
                    '/article/review/queryReviewer',
                    query,
                    50,
                    function(array){
                        array.forEach(
                            function(d ){
                                d.statis = d.total + "/" + d.completed;
                                d.src = (d.source == 1 ? "User" : "Reviewer");
                            }
                        )
                        return array;
                    },
                    function(dom){
                        dom.find('.row').click(function(){
                            var data = {};
                            data.name = $(this).attr('name');
                            data.statis = $(this).attr('statis');
                            data.email = $(this).attr('email');
                            _that.addReviewers(data);
                        })
                    },'email,name,statis'
                );
            }
        }

        return Reviewer;
    }
)