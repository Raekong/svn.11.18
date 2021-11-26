define(
    [],
    function(){
        var ds ={
            section_ds : {
                'data-title': {
                    'verify': 'nonull',
                    'val' : ''
                },
        
                'data-sectionEditor': {
                    'verify': 'nonull',
                    'val' : ''
                },
        
                'data-expireDay': {
                    'verify': 'date',
                    'val' : ''
                }
            },
            masthead_ds:  {
                'data-researchfield': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-i18n':{
                    'verify': '',
                    'val' : 'en'
                },
                'data-onlineissn':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-printissn':{
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-host':{
                    'verify': '',
                    'val' : ''
                },
                'data-port':{
                    'verify': 'int',
                    'val' : ''
                },
                'data-email':{
                    'verify': 'email',
                    'val' : ''
                },
                'data-emailsender':{
                    'verify': '',
                    'val' : ''
                },
                'data-password':{
                    'verify': '',
                    'val' : ''
                }
            },
        
            contact_ds : {
                'data-name': {
                    'verify': 'nonull',
                    'val' : ''
                },
        
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },
        
                'data-title1':{
                    'verify': '',
                    'val' : ''
                },
        
                'data-phone':{
                    'verify': '',
                    'val' : ''
                },
        
                'data-affiliation': {
                    'verify': '',
                    'val' : ''
                },
        
                'data-techname': {
                    'verify': 'nonull',
                    'val' : ''
                },
        
                'data-techemail':{
                    'verify': 'email',
                    'val' : ''
                },
        
                'data-techphone':{
                    'verify': '',
                    'val' : ''
                }
            }
        }
        return ds;
    }
)
