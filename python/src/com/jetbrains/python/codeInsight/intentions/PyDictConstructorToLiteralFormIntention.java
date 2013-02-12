package com.jetbrains.python.codeInsight.intentions;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;

/**
 * User: catherine
 * Intention to convert dict constructor to dict literal expression with only explicit keyword arguments
 * For instance,
 * dict() -> {}
 * dict(a=3, b=5) -> {'a': 3, 'b': 5}
 * dict(foo) -> no transformation
 * dict(**foo) -> no transformation
 */
public class PyDictConstructorToLiteralFormIntention extends BaseIntentionAction {
  @NotNull
  public String getFamilyName() {
    return PyBundle.message("INTN.convert.dict.constructor.to.dict.literal");
  }

  @NotNull
  public String getText() {
    return PyBundle.message("INTN.convert.dict.constructor.to.dict.literal");
  }

  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PyCallExpression expression =
      PsiTreeUtil.getParentOfType(file.findElementAt(editor.getCaretModel().getOffset()), PyCallExpression.class);
    
    if (expression != null && expression.isCalleeText("dict")) {
      final TypeEvalContext context = TypeEvalContext.fast();
      PyType type = context.getType(expression);
      if (type != null && type.isBuiltin(context)) {
        PyExpression[] argumentList = expression.getArguments();
        for (PyExpression argument : argumentList) {
          if (!(argument instanceof PyKeywordArgument)) return false;
        }
        return true;
      }
    }
    return false;
  }

  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PyCallExpression expression =
          PsiTreeUtil.getParentOfType(file.findElementAt(editor.getCaretModel().getOffset()), PyCallExpression.class);
    PyElementGenerator elementGenerator = PyElementGenerator.getInstance(project);
    if (expression != null) {
      replaceDictConstructor(expression, elementGenerator);
    }
  }

  private static void replaceDictConstructor(PyCallExpression expression, PyElementGenerator elementGenerator) {
    PyExpression[] argumentList = expression.getArguments();
    StringBuilder stringBuilder = new StringBuilder();

    int size = argumentList.length;

    for (int i = 0; i != size; ++i) {
      PyExpression argument = argumentList[i];
      if (argument instanceof PyKeywordArgument) {
        stringBuilder.append("'");
        stringBuilder.append(((PyKeywordArgument)argument).getKeyword());
        stringBuilder.append("' : ");
        stringBuilder.append(((PyKeywordArgument)argument).getValueExpression().getText());
        if (i != size-1)
          stringBuilder.append(",");
      }

    }
    PyDictLiteralExpression dict = (PyDictLiteralExpression)elementGenerator.createFromText(LanguageLevel.forElement(expression), PyExpressionStatement.class,
                                                "{" + stringBuilder.toString() + "}").getExpression();
    expression.replace(dict);
  }
}
