package info.piwai.buildergen.modeling;

import static com.sun.codemodel.JExpr._this;
import info.piwai.buildergen.api.Buildable;
import info.piwai.buildergen.api.Builder;
import info.piwai.buildergen.helper.ElementHelper;
import info.piwai.buildergen.processing.BuilderGenProcessor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

public class ModelBuilder {

	private final ElementHelper elementHelper;

	public ModelBuilder(ElementHelper elementHelper) {
		this.elementHelper = elementHelper;
	}

	public JCodeModel build(Set<TypeElement> validatedElements) throws JClassAlreadyExistsException {
		JCodeModel codeModel = new JCodeModel();
		for (TypeElement buildableElement : validatedElements) {
			String builderFullyQualifiedName = extractBuilderFullyQualifiedName(buildableElement);

			Set<ExecutableElement> constructors = elementHelper.findAccessibleConstructors(buildableElement);

			ExecutableElement constructor = elementHelper.findBuilderConstructor(constructors);

			JDefinedClass builderClass = codeModel._class(builderFullyQualifiedName);

			JClass buildableClass = codeModel.ref(buildableElement.getQualifiedName().toString());

			JClass builderInterface = codeModel.ref(Builder.class);
			JClass narrowedInterface = builderInterface.narrow(buildableClass);

			builderClass._implements(narrowedInterface);

			SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			builderClass.annotate(Generated.class) //
					.param("value", BuilderGenProcessor.class.getName()) //
					.param("date", isoDateFormat.format(new Date())) //
					.param("comments", "Generated by BuilderGen (https://github.com/pyricau/BuilderGen)");

			List<? extends VariableElement> parameters = constructor.getParameters();
			for (VariableElement parameter : parameters) {
				String paramName = parameter.getSimpleName().toString();
				String paramClassFullyQualifiedName = parameter.asType().toString();
				JClass paramClass = codeModel.ref(paramClassFullyQualifiedName);
				JFieldVar setterField = builderClass.field(JMod.PRIVATE, paramClass, paramName);

				JMethod setter = builderClass.method(JMod.PUBLIC, builderClass, paramName);
				JVar setterParam = setter.param(paramClass, paramName);
				setter.body() //
						.assign(_this().ref(setterField), setterParam) //
						._return(_this());

				JDocComment javadoc = setter.javadoc() //
						.append("Setter for the ") //
						.append(paramName) //
						.append(" parameter.");
				
				javadoc.addParam(setterParam) //
						.append("the value for the ") //
						.append(paramName) //
						.append(" constructor parameter of the ") //
						.append(buildableClass) //
						.append(" class.");

				javadoc.addReturn() //
						.append("this, ie the ") //
						.append(builderClass) //
						.append(" instance, to enable chained calls.");
			}

			JMethod buildMethod = builderClass.method(JMod.PUBLIC, buildableClass, "build");
			
			List<? extends TypeMirror> thrownTypes = constructor.getThrownTypes();
			for (TypeMirror thrownType : thrownTypes) {
				JClass thrownClass = codeModel.ref(thrownType.toString());
				buildMethod._throws(thrownClass);
			}

			JBlock buildBody = buildMethod.body();
			JInvocation newBuildable = JExpr._new(buildableClass);

			for (VariableElement parameter : constructor.getParameters()) {
				String paramName = parameter.getSimpleName().toString();
				newBuildable.arg(JExpr.ref(paramName));
			}

			buildBody._return(newBuildable);

			JMethod createMethod = builderClass.method(JMod.PUBLIC | JMod.STATIC, builderClass, "create");
			createMethod.body()._return(JExpr._new(builderClass));

			builderClass.javadoc() //
					.append("Builder for the ") //
					.append(buildableClass) //
					.append(" class.<br />\n") //
					.append("<br />\n") //
					.append("This builder implements Joshua Bloch's builder pattern, to let you create ") //
					.append(buildableClass) //
					.append(" instances \nwithout having to deal with complex constructor parameters.\n") //
					.append("Furthermore, this builder has a fluid interface, which mean you can chain calls to its methods.<br />\n") //
					.append("<br />\n") //
					.append("You may create a new builder by calling the ") //
					.append("{@link #create()}") //
					.append(" static method, or this builder's constructor.<br />\n") //
					.append("<br />\n") //
					.append("When done with settings fields, you can create ") //
					.append(buildableClass) //
					.append(" instances by calling the ") //
					.append("{@link #build()}") //
					.append(" method.") //
					.append("Each call will return a new instance.") //
					.append("You can call setters multiple times, and use this builder as an object template.");
		}
		return codeModel;
	}

	private String extractBuilderFullyQualifiedName(TypeElement buildableElement) {
		Buildable buildableAnnotation = buildableElement.getAnnotation(Buildable.class);
		String builderSuffix = buildableAnnotation.value();

		String buildableFullyQualifiedName = buildableElement.getQualifiedName().toString();

		return buildableFullyQualifiedName + builderSuffix;
	}

}
