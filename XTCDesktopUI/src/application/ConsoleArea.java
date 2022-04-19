package application;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.fxmisc.richtext.StyleClassedTextArea;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public class ConsoleArea extends BorderPane {
	{
		getStyleClass().add("console-area");
		getStylesheets().add(ConsoleArea.class.getResource("view/console-area.css").toExternalForm());
	}

	private ConsoleInput input;
	private ConsoleOutput output;
	private ArrayList<String> history;
	private int historyPointer = 0;

	public ConsoleArea() {
		history = new ArrayList<>();
		input = new ConsoleInput();
		input.addEventHandler(KeyEvent.KEY_RELEASED, ev -> {
			switch (ev.getCode()) {
			case ENTER:
				invokeCmd(input.getText());
				input.clear();
				break;
			case UP:
				historyPointer--;
				if (historyPointer < 0) {
					historyPointer = 0;
				}
				input.setText(history.get(historyPointer));
				input.positionCaret(input.getText().length());
				break;
			case DOWN:
				historyPointer++;
				if (historyPointer >= history.size()) {
					historyPointer = history.size();
					return;
				}
				input.setText(history.get(historyPointer));
				input.positionCaret(input.getText().length());
				break;
			default:
				break;
			}
		});
		output = new ConsoleOutput();
		output.redirect();

		setCenter(output);
		setBottom(input);
	}

	public void invokeCmd(String cmd) {
		if (cmd.trim().isEmpty()) {
			return;
		}
		history.add(cmd);
		historyPointer = history.size();
		output.appendText("$ "+cmd);
		output.appendText(System.lineSeparator());
		output.scrollYToPixel(Double.MAX_VALUE);
	}

	private class ConsoleOutput extends StyleClassedTextArea {
		{
			getStyleClass().add("console-output");
			getStylesheets().add(ConsoleArea.class.getResource("view/console-area.css").toExternalForm());
			setUseInitialStyleForInsertion(true);
		}

		public ConsoleOutput() {
			editableProperty().set(false);
		}

		public void redirect() {
			System.setOut(new PrintStream(new OutputRedirector(this)));
		}

		private class OutputRedirector extends OutputStream {
			private ConsoleOutput parent;

			public OutputRedirector(ConsoleOutput parent) {
				this.parent = parent;
			}

			@Override
			public void write(int b) throws IOException {
				Platform.runLater(()-> {
					parent.appendText(String.valueOf((char) b));	
					parent.scrollYToPixel(Double.MAX_VALUE);
				});
			}

		}
	}

	private class ConsoleInput extends TextField {
		{
			getStyleClass().add("console-input");
			getStylesheets().add(ConsoleArea.class.getResource("view/console-area.css").toExternalForm());
		}
	}
}
