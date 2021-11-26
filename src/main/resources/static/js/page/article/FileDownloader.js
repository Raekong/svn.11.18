define(
    [],
    function(  ){
        function FileDownloader( dom ){
            this.dom = dom;

            var _that = this;

            this.init = function(){
                this.dom.find("#downloadZip").click(
                    function(){
                        console.log('fdsaf============-------------');
                        var selected = $('.row span.checked');
                        if( selected.length == 0 ) return;
                        var tmp = [];
                        for(var i=0; i<selected.length; i++){
                            tmp.push(selected.eq(i).closest('.row').attr('data'));
                        }
                        var fileStr = tmp.join(";");
                        window.open("/article/file/download?appends="+fileStr);
                    }
                )
                
                this.dom.find('#selectall').click(
                    function(){
                        _that.dom.find('.row span.nocheck').removeClass('checked').addClass('checked');
                    }
                )
    
                this.dom.find('.cell.file').click(
                    function(){
                        var link = $(this).attr('href');
                        var fileName = $(this).text();
                        _that.download(link, fileName);
                    }
                )
    
                this.dom.find('.row span.nocheck').click(
                    function(){
                        if($(this).hasClass('checked')){
                            $(this).removeClass('checked')
                        }else{
                            $(this).addClass('checked')
                        }
                    }
                )
            }
            _that.download = function(url, filename){
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


            

            
        }

        return FileDownloader;
    }
)