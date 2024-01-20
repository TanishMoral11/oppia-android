package org.oppia.android.scripts.todo

import com.google.protobuf.TextFormat
import kotlinx.coroutines.runBlocking
import org.oppia.android.scripts.common.GitHubClient
import org.oppia.android.scripts.common.ScriptBackgroundCoroutineDispatcher
import org.oppia.android.scripts.common.model.GitHubIssue
import org.oppia.android.scripts.proto.TodoOpenExemption
import org.oppia.android.scripts.proto.TodoOpenExemptions
import org.oppia.android.scripts.todo.model.Todo
import java.io.File
import java.io.FileInputStream

/**
 * Script for ensuring that all TODOs present in the repository are correctly formatted and
 * corresponds to open issues on GitHub.
 *
 * Note that the setup instructions at
 * https://github.com/oppia/oppia-android/wiki/Static-Analysis-Checks#todo-open-checks must be
 * followed in order to ensure that this script works correctly in the local environment.
 *
 * Usage:
 *   bazel run //scripts:todo_open_check -- <path_to_dir_root> <path_to_proto_binary> [regenerate]
 *
 * Arguments:
 * - path_to_dir_root: directory path to the root of the Oppia Android repository.
 * - path_to_proto_binary: relative path to the exemption .pb file.
 * - regenerate: optional 'regenerate' string to, instead of checking for TODOs, regenerate the
 *     exemption textproto file and print it to the command output.
 *
 * Examples:
 *   bazel run //scripts:todo_open_check -- $(pwd) scripts/assets/todo_open_exemptions.pb
 *   bazel run //scripts:todo_open_check -- $(pwd) scripts/assets/todo_open_exemptions.pb regenerate
 */
fun main(vararg args: String) {
  // Path of the repo to be analyzed.
  val repoRoot = File(args[0]).absoluteFile.normalize()
  val repoPath = "${repoRoot.path}/"

  val pathToProtoBinary = args[1]
  val regenerateFile = args.getOrNull(2) == "regenerate"

  val todoExemptionTextProtoFilePath = "scripts/assets/todo_exemptions"

  // List of all the open issues on GitHub of this repository.
  val openIssueList = ScriptBackgroundCoroutineDispatcher().use { scriptBgDispatcher ->
    val gitHubClient = GitHubClient(repoRoot, scriptBgDispatcher)
    runBlocking { gitHubClient.fetchAllOpenIssuesAsync().await() }
  }

  val todoExemptionList =
    loadTodoExemptionsProto(pathToProtoBinary).getTodoOpenExemptionList()

  val allTodos = TodoCollector.collectTodos(repoPath)

  val poorlyFormattedTodos = TodoCollector.collectPoorlyFormattedTodos(allTodos)

  val correctlyFormattedTodos = TodoCollector.collectCorrectlyFormattedTodos(
    allTodos - poorlyFormattedTodos
  )

  val openIssueFailureTodos = correctlyFormattedTodos.filter { todo ->
    checkIfIssueDoesNotMatchOpenIssue(codeLine = todo.lineContent, openIssueList = openIssueList)
  }

  val redundantExemptions = retrieveRedundantExemptions(
    todos = poorlyFormattedTodos + openIssueFailureTodos, todoExemptionList, repoRoot
  )

  val poorlyFormattedTodosAfterExemption =
    retrieveTodosAfterExemption(todos = poorlyFormattedTodos, todoExemptionList, repoRoot)

  val openIssueFailureTodosAfterExemption =
    retrieveTodosAfterExemption(todos = openIssueFailureTodos, todoExemptionList, repoRoot)

  logRedundantExemptions(redundantExemptions, todoExemptionTextProtoFilePath)

  logFailures(
    invalidTodos = poorlyFormattedTodosAfterExemption,
    repoRoot,
    failureMessage = "TODOs not in correct format:",
  )

  logFailures(
    invalidTodos = openIssueFailureTodosAfterExemption,
    repoRoot,
    failureMessage = "TODOs not corresponding to open issues on GitHub:",
  )

  if (poorlyFormattedTodosAfterExemption.isNotEmpty() ||
    openIssueFailureTodosAfterExemption.isNotEmpty()
  ) {
    println(
      "Refer to https://github.com/oppia/oppia-android/wiki/Static-Analysis-Checks" +
        "#todo-open-checks for more details on how to fix this.\n"
    )
  }

  if (regenerateFile) {
    println("Regenerated exemptions:")
    println()
    val allProblematicTodos = poorlyFormattedTodos + openIssueFailureTodos
    val newExemptions = allProblematicTodos.convertToExemptions(repoRoot)
    println(newExemptions.convertToExemptionTextProto())
    throw Exception("TODO CHECK SKIPPED")
  }

  if (
    redundantExemptions.isNotEmpty() ||
    poorlyFormattedTodosAfterExemption.isNotEmpty() ||
    openIssueFailureTodosAfterExemption.isNotEmpty()
  ) {
    println(
      "There were failures. Re-run the command with \"regenerate\" at the end to regenerate the" +
        " exemption file with all failures as exempted."
    )
    println()
    throw Exception("TODO CHECK FAILED")
  } else {
    println("TODO CHECK PASSED")
  }
}

/**
 * Retrieves the TODO open check failures list after filtering them from the exemptions.
 *
 * @param todos the list of all the failure causing TODOs
 * @param todoExemptionList the list contating the TODO exemptions
 * @param repoRoot the root directory of the repository
 * @return list obtained after filtering the exemptions
 */
private fun retrieveTodosAfterExemption(
  todos: List<Todo>,
  todoExemptionList: List<TodoOpenExemption>,
  repoRoot: File
): List<Todo> {
  return todos.filter { todo ->
    todoExemptionList.none {
      it.exemptedFilePath == todo.file.toRelativeString(repoRoot) &&
        todo.lineNumber in it.lineNumberList
    }
  }
}

/**
 * Retrieves a list of redundant exemptions.
 *
 * @param todos the list of all the failure causing TODOs
 * @param todoExemptionList the list contating the TODO exemptions
 * @param repoRoot the root directory of the repository
 * @return a list of all the redundant exemptions
 */
private fun retrieveRedundantExemptions(
  todos: List<Todo>,
  todoExemptionList: List<TodoOpenExemption>,
  repoRoot: File
): List<Pair<String, Int>> {
  return todoExemptionList.flatMap { exemption ->
    exemption.lineNumberList.mapNotNull { exemptedLineNumber ->
      val isRedundantExemption = todos.none {
        it.file.toRelativeString(repoRoot) == exemption.exemptedFilePath &&
          it.lineNumber == exemptedLineNumber
      }
      if (isRedundantExemption) {
        Pair(exemption.exemptedFilePath, exemptedLineNumber)
      } else {
        null
      }
    }
  }
}

/**
 * Checks whether a TODO does not corresponds to open issues on GitHub.
 *
 * @param codeLine the line of code to be checked
 * @param openIssueList the list of all the open issues of this repository on GitHub
 * @return whether the TODO does not corresponds to open issues on GitHub
 */
private fun checkIfIssueDoesNotMatchOpenIssue(
  codeLine: String,
  openIssueList: List<GitHubIssue>,
): Boolean {
  val parsedIssueNumberFromTodo = TodoCollector.parseIssueNumberFromTodo(codeLine)
  return openIssueList.none { it -> it.number == parsedIssueNumberFromTodo }
}

/**
 * Logs the redundant exemptions.
 *
 * @param redundantExemptions list of redundant exemptions
 * @param todoExemptionTextProtoFilePath the location of the TODO exemption textproto file
 */
private fun logRedundantExemptions(
  redundantExemptions: List<Pair<String, Int>>,
  todoExemptionTextProtoFilePath: String
) {
  if (redundantExemptions.isNotEmpty()) {
    println("Redundant exemptions (there are no TODOs corresponding to these lines):")
    redundantExemptions.sortedWith(compareBy({ it.first }, { it.second })).forEach { exemption ->
      println("- ${exemption.first}:${exemption.second}")
    }
    println(
      "Please remove them from $todoExemptionTextProtoFilePath.textproto"
    )
    println()
  }
}

/**
 * Logs the TODO open check failures.
 *
 * @param invalidTodos a list of all the invalid TODOs present in the repository. A TODO is
 *     considered to be invalid if it is poorly formatted or if it does not corresponds to open
 *     issues on GitHub.
 * @param repoRoot the root directory of the repository
 * @param failureMessage the failure message to be logged
 */
private fun logFailures(invalidTodos: List<Todo>, repoRoot: File, failureMessage: String) {
  if (invalidTodos.isNotEmpty()) {
    println(failureMessage)
    invalidTodos.sortedWith(compareBy({ it.file.path }, { it.lineNumber })).forEach {
      println("- ${it.file.toRelativeString(repoRoot)}:${it.lineNumber}")
    }
    println()
  }
}

private fun List<Todo>.convertToExemptions(repoRoot: File): List<TodoOpenExemption> {
  return groupBy { it.file.path }.map { (_, todos) ->
    TodoOpenExemption.newBuilder().apply {
      exemptedFilePath = todos.first().file.toRelativeString(repoRoot)
      addAllLineNumber(todos.map { it.lineNumber }.sorted())
    }.build()
  }.sortedBy { it.exemptedFilePath }
}

private fun List<TodoOpenExemption>.convertToExemptionTextProto(): String {
  val baseProto = TodoOpenExemptions.newBuilder().apply {
    addAllTodoOpenExemption(this@convertToExemptionTextProto)
  }.build()
  return TextFormat.printer().printToString(baseProto)
}

/**
 * Loads the TODO open check exemptions list corresponding to a text proto file.
 *
 * @param pathToProtoBinary the location of the exemption textproto file
 * @return proto class from the parsed textproto file
 */
private fun loadTodoExemptionsProto(pathToProtoBinary: String): TodoOpenExemptions {
  val protoBinaryFile = File(pathToProtoBinary)
  val builder = TodoOpenExemptions.getDefaultInstance().newBuilderForType()

  // This cast is type-safe since proto guarantees type consistency from mergeFrom(),
  // and this method is bounded by the generic type T.
  @Suppress("UNCHECKED_CAST")
  val protoObj: TodoOpenExemptions =
    FileInputStream(protoBinaryFile).use {
      builder.mergeFrom(it)
    }.build() as TodoOpenExemptions
  return protoObj
}
