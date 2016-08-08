/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.validation.script;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.exceptions.ValidationException;
import com.consol.citrus.message.Message;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.script.ScriptTypes;
import com.consol.citrus.validation.AbstractMessageValidator;
import com.consol.citrus.validation.context.ValidationContext;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 * Groovy script message validator passing the message to a validation script.
 * Tester needs to write validation code in Groovy.
 * 
 * Available objects inside groovy script are 'receivedMessage' which is the actual {@link Message} object
 * to validate and 'context' the current {@link TestContext}.
 * 
 * @author Christoph Deppisch
 */
public class GroovyScriptMessageValidator extends AbstractMessageValidator<ScriptValidationContext> {

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(GroovyScriptMessageValidator.class);
    
    /** Static code snippet for groovy script validation */
    private Resource scriptTemplateResource;
    
    /**
     * Default constructor using default script template.
     */
    public GroovyScriptMessageValidator() {
        this(new ClassPathResource("com/consol/citrus/validation/script-validation-template.groovy"));
    }
    
    /**
     * Constructor setting the script template for this validator.
     * @param scriptTemplateResource the script template to use in this validator.
     */
    public GroovyScriptMessageValidator(ClassPathResource scriptTemplateResource) {
        this.scriptTemplateResource = scriptTemplateResource;
    }

    @Override
    public void validateMessage(Message receivedMessage, Message controlMessage, TestContext context, ScriptValidationContext validationContext)
        throws ValidationException {
        try {
            String validationScript = validationContext.getValidationScript(context);
            
            if (StringUtils.hasText(validationScript)) {
                log.debug("Start groovy message validation");

                GroovyClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
                    public GroovyClassLoader run() {
                        return new GroovyClassLoader(GroovyScriptMessageValidator.class.getClassLoader());
                    }
                });
                Class<?> groovyClass = loader.parseClass(TemplateBasedScriptBuilder.fromTemplateResource(scriptTemplateResource)
                                                            .withCode(validationScript)
                                                            .build());
                
                if (groovyClass == null) {
                    throw new CitrusRuntimeException("Failed to load groovy validation script resource");
                }
                
                GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
                ((GroovyScriptExecutor) groovyObject).validate(receivedMessage, context);
                
                log.info("Groovy message validation successful: All values OK");
            }
        } catch (CompilationFailedException e) {
            throw new CitrusRuntimeException(e);
        } catch (InstantiationException e) {
            throw new CitrusRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new CitrusRuntimeException(e);
        } catch (AssertionError e) {
            throw new ValidationException("Groovy script validation failed with assertion error:\n" + e.getMessage(), e);
        }
    }

    @Override
    protected ScriptValidationContext findValidationContext(List<ValidationContext> validationContexts) {
        for (ValidationContext validationContext : validationContexts) {
            if (getRequiredValidationContextType().isInstance(validationContext) &&
                    ((ScriptValidationContext)validationContext).getScriptType().equals(ScriptTypes.GROOVY)) {
                return (ScriptValidationContext) validationContext;
            }
        }
        
        return null;
    }

    @Override
    protected Class<ScriptValidationContext> getRequiredValidationContextType() {
        return ScriptValidationContext.class;
    }

    @Override
    public boolean supportsMessageType(String messageType, Message message) {
        // support all known message types other than XML
        return MessageType.knows(messageType) && !messageType.equalsIgnoreCase(MessageType.XML.toString())
                && !messageType.equalsIgnoreCase(MessageType.XHTML.toString());
    }
}
