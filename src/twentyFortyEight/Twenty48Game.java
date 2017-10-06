package twentyFortyEight;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
/*
 * Author: David Jones
 * Date: Mar. 2, 2017 
 * 
 * Class represents a game of 2048. 
 * 
 * 2048 is typically a 4x4 board of numbers. The goal of the game is to combine like numbers by moving left, right, up or down
 * to obtain the value of 2048. When the game starts two numbers (a 2 or 4) are randomly placed on the board
 * Each time the player completes a move a single 2 or 4 is spawned into a random location. This version allows the game board to be
 * set at initialization from [3-16] x [3-16].
 * 
 * This class contains methods to control the game allowing for spawning a random number, making
 * a move vertically (up or down) and horizontally (right or left), undoing up to 10 moves, getting the status of the game, and displaying
 * the board via the console. 
 * 
 * It spawns random numbers by generating a 2 90% of the time and a 4 10% of the time. A list is populated
 * of all open cells and a cell to place the random number is randomly selected from the list. 
 * 
 * Moves are made by shifting all cells in the direction of movement removing any empty (0 valued cells) that exist between cells 
 * that are not empty (non 0 valued). Same valued cells that are adjacent are merged and added to a score. 
 * and selecting a random index
 * 
 * Moves are undone by restoring the game state, board, score and move count of the previous move.
 * 
 * The status of the game corresponds to the last executed move. The status determines whether the game is playable, whether the last 
 * move was a winning move, whether the game has been won and is still playable and whether the game has been lost.
 * 
 * The board is displayed via the console with a double edged border and cells being divided with single lined borders.
 * 
 * Typically the class is used in the following order. Constructor is called initializing the game to a specified number of rows and columns.
 * A loop is executed that makes calls to this class to display the board, move a piece, and check the status
 * then loop until the game is over or ended by the user.
 * 
 * The class is marked as public so it can be used outside the package. 
 */
import java.util.ArrayList;
import java.util.Stack;

public class Twenty48Game
{
	public enum GameStatus					//Used to indicate the status of the game
	{
		LOST,								//No moves can be made
		WIN,								//The move made resulted in a win
		WON_BUT_STILL_PLAYABLE,				//User has already won but still can make moves			
		WON_BUT_UNPLAYABLE,					//User has already won but is now out of moves
		PLAYABLE,							//The game has not been won, possible moves exist
		}
	
	public static final int MAX_BOARD_DIMENSION = 16;		//The maximum number of rows or columns the board can have
	public static final int MIN_BOARD_DIMENSION = 3;		//The minimum number of rows or columns the board can have
	
	public final int TOTAL_ROWS;									
	public final int TOTAL_COLUMNS;	
	
    public Twenty48Game	(int numberOfRows, int numberOfColumns)
	{
    	/*
    	 *	Constructor initializes the current board. The constructor also creates an empty list of open cells
    	 *  and spawns two pieces.
    	 */
    	TOTAL_ROWS = numberOfRows;
		TOTAL_COLUMNS = numberOfColumns;
		
		//Make capacity of the save states 11 so 10 undo moves are possible. 11 is chosen because when 10 saved states already exist
		//an eleventh one is pushed onto the stack before the bottom one is removed
		savedStates.ensureCapacity(11);										
		currentBoard = new int[numberOfRows][numberOfColumns];
		openCells = new ArrayList<Integer>(numberOfRows * numberOfColumns);	//Make the capacity of the array list the total number of cells
		spawn();		//Spawn two pieces
		spawn();		
	}
    
    public Twenty48Game(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
    {
    	/*
    	 * Constructor reads saved data from the input stream and initializes the board
    	 */
    
    	//Make capacity of the save states 11 so 10 undo moves are possible. 11 is chosen because when 10 saved states already exist
    	//an eleventh one is pushed onto the stack before the bottom one is removed
    	savedStates.ensureCapacity(11);
    	
    	//Read the current board
    	currentBoard = (int[][])inputStream.readObject();
    	this.TOTAL_ROWS = currentBoard.length;
    	this.TOTAL_COLUMNS = currentBoard[0].length;
    	
    	//Make the capacity of the array list the total number of cells
    	openCells = new ArrayList<Integer>(TOTAL_ROWS * TOTAL_COLUMNS);
    	
    	//Read the saved boards
    	int totalSavedStates = inputStream.readInt();
    	for(int i = 0; i < totalSavedStates; i ++)
    		savedStates.push(new GameState(inputStream));
    	
    	//Read the saved move count, score and status
    	moveCount = inputStream.readInt();
    	currentScore = inputStream.readInt();
    	currentStatus = (GameStatus)inputStream.readObject();
    }
    
    public boolean isUndoPossible()
    {
    	/*
    	 * Returns whether an undo can be successfully performed
    	 */
    	
    	//if there are no saved states then return  false
    	return (savedStates.size() != 0);
    }
    
    public void displayBoard	()
	{
    	/*
    	 * Prints the game board on the console with a pretty boarder. Accommodates for cells that have a width up to 4 numbers. Also
    	 * prints the move count and current score.
    	 */
    	
		//Print the top border. The top border will have double border style. 
		System.out.print("\u2554");									//Top Left Double Border
		for(int column = 0; column < TOTAL_COLUMNS - 1; column ++)
			System.out.print("\u2550\u2550\u2550\u2550\u2564");		// 4 '=' and one top double border with downward single vertical bar
		System.out.println("\u2550\u2550\u2550\u2550\u2557");		// 4 '=' and 1 top right double corner
		
		//Display the rows and their bottom borders. Do not display last row BOTTOM BORDER in the loop, it is different
		//(last row values are displayed in the loop)
		for(int row = TOTAL_ROWS - 1; row >= 0; row --)							
		{
			System.out.print('\u2551');	 //	 	'||'
			
			//Display the cells except the last one with a vertical border '|' in the current row. 
			for(int column = 0; column < TOTAL_COLUMNS - 1; column ++)	
				if(currentBoard[row][column] != 0)		System.out.printf("%4d\u2502",currentBoard[row][column]);	
				else									System.out.print("    \u2502");	
			
			//Display the last cell of the current row with a double vertical boarder '||'. 
			if(currentBoard[row][TOTAL_COLUMNS -1] != 0)	System.out.printf("%4d\u2551%n",currentBoard[row][TOTAL_COLUMNS - 1]);
			else										System.out.println("    \u2551");						
			if(row == 0)	break;	//Is this the last row? If so exit the loop because the bottom border is different
	
			//Display the bottom border of the current row
			System.out.print("\u255F");									// 		'||-'
			for(int column = 0; column < TOTAL_COLUMNS - 1; column ++)			
				System.out.print("\u2500\u2500\u2500\u2500\u253C");		// 4 	'-' and 1 '-|-'
			System.out.println("\u2500\u2500\u2500\u2500\u2562");		// 4 	'-' and  1 '-||'
		}
		//Display the bottom border of board
		System.out.print("\u255A");										// Bottom left double corner
		for(int column = 0; column < TOTAL_COLUMNS - 1; column ++)			
			System.out.print("\u2550\u2550\u2550\u2550\u2567");			// 4	'=' and double border with single vertical bar
		System.out.println("\u2550\u2550\u2550\u2550\u255D");			// 4	'=' and the bottom right corner
		System.out.println("Moves Made: " + moveCount + " Score: " + currentScore);	//Display move count and score
	}
   
    public final boolean moveDown () 
    {
    	/*
    	 * Shifts all cells down, combines cells, and calculates the new score. Spawns a new piece if the move was successful
    	 * Returns true if the move was made, false if the move is not possible
    	 */
    	
    	boolean wasShifted = false;	//Determines if any cells have been changed.

    	//Go through each column and shift and combine cells in a downward direction
    	for(int column = 0; column < TOTAL_COLUMNS; column ++)				
    	{
    		int zeroIndex, count; 	//zeroIndex is the row index of a zero valued cell. Count is the number of subsequent non-zero cells
    		
    		//Find the first cell with a zero searching from bottom to top (loop does not contain a body)					
    		for(zeroIndex = 0; zeroIndex < TOTAL_ROWS - 1 && currentBoard[zeroIndex][column] != 0; zeroIndex ++);
    		
    		//Find a cell above zeroIndex that has a non-zero value. Move subsequent non-zero cells to the location of the non-zero value
    		for(int nonZeroIndex = zeroIndex + 1; nonZeroIndex < TOTAL_ROWS; nonZeroIndex ++) 
    		{
    			//Does the cell contain a 0? If so skip it
    			if(currentBoard[nonZeroIndex][column] == 0)	continue; 	
    			
    			//Has the board not been changed yet? If so save the state of the game
    			if(!wasShifted)			
    			{
    				saveCurrentState();
    				wasShifted = true;						
    			}
    			
    			//Move all subsequent non-zero cells above [nonZeroIndex][column] down to [zeroIndex][column].
    			for(count = 0;nonZeroIndex + count < TOTAL_ROWS && currentBoard[nonZeroIndex + count][column] != 0; count ++) 
    			{
    				currentBoard[zeroIndex + count][column] = currentBoard[nonZeroIndex + count][column];
    				currentBoard[nonZeroIndex + count][column] = 0;
    			}
    			
    			//[zeroIndex][column] -> [zeroIndex + count][column] are in proper position. [zeroIndex + count][column] now contains a 0
    			nonZeroIndex = (zeroIndex += count);	 
    		}	
    		//Combine the cells with the same value next to each other in the current column. Once a zero valued cell has been reached all
    		//other cells after it are guaranteed to be a 0
    		for(int row = 0; row < TOTAL_ROWS - 1 && currentBoard[row][column] != 0; row ++)	
    		{
    			//Is the current cell different then the cell following it? If so skip it
    			if(currentBoard[row][column] != currentBoard[row + 1][column]) 	continue;
    			
    			//Has the board not been changed yet? If so save the state of the game
    			if(!wasShifted) 		
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift cells above [row+1][column] down one position. Set the top most cell to 0
    			for(int i = row + 1; i < TOTAL_ROWS - 1;i ++)								 
    				currentBoard[i][column] = currentBoard[i + 1][column];			
    			currentBoard[TOTAL_ROWS- 1][column] = 0;		
    			
    			//Double the cell value since it was merged and add to the current score
    			currentScore += (currentBoard[row][column] <<= 1);					
    		}
    	}
    	//Was the board not changed? If so then the move was not possible
    	if(!wasShifted)	return false;
    	moveCount ++;	//At this point in code the move was possible. Spawn a new piece
    	spawn();
    	return true;
    }
    
    public final boolean moveLeft () 
    {
    	/*
    	 * Shifts all cells left, combines cells, and calculates the new score. Spawns a new piece if the move was successful
    	 * Returns true if the move was made, false if the move is not possible
    	 */
    	
    	boolean wasShifted = false;	//Determines if any cells been changed.
    	
    	//Go through each row and shift and combine cells in a left direction
    	for(int row = 0; row < TOTAL_ROWS; row ++)						
    	{
    		int zeroIndex, count;//zeroIndex is the column index of a zero valued cell. Count is the number of subsequent non-zero cells
    		 
    		//Find the first cell with a 0 in it searching from left to right (loop does not contain a body)	
    		for(zeroIndex = 0;zeroIndex < TOTAL_COLUMNS - 1 && currentBoard[row][zeroIndex] != 0; zeroIndex ++);	
    		
    		//Find the next non-zero cell to the right of zeroIndex. Move all subsequent non-zero cells to the location of the non-zero cell
    		for(int nonZeroIndex = zeroIndex + 1; nonZeroIndex < TOTAL_COLUMNS; nonZeroIndex ++)		
    		{
    			//Does the current cell contain a 0? If so skip it
    			if(currentBoard[row][nonZeroIndex] == 0)	continue;
    			
    			//Has the board not been changed yet? If so save the state of the game
    			if(!wasShifted)		
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift all subsequent non-zero cells to the right of [row][nonZeroIndex] to [row][zeroIndex]
    			for(count = 0;nonZeroIndex + count < TOTAL_COLUMNS && currentBoard[row][nonZeroIndex + count] != 0; count ++) 
    			{
    				currentBoard[row][zeroIndex + count] = currentBoard[row][nonZeroIndex + count];
    				currentBoard[row][nonZeroIndex + count] = 0;
    			}
    			
    			//[row][zeroIndex] -> [row][zeroIndex + count] are in proper position. [row][zeroIndex + count] now contains a 0
    			nonZeroIndex = (zeroIndex += count);							
    		}
    		//Combine the same cells next to each other in the current row. Once a cell has a 0 the rest are guaranteed to have 0's
    		for(int column = 0; column < TOTAL_COLUMNS - 1 && currentBoard[row][column] != 0; column ++)
    		{
    			//Is the current cell different then the cell to the right of it? If so skip it
    			if(currentBoard[row][column] != currentBoard[row][column + 1]) 	continue;
    			
    			//Has the board NOT been changed yet? If so save the state of the game
    			if(!wasShifted)			
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Move cells right of [row][column + 1] left one position
    			for(int i = column + 1; i < TOTAL_COLUMNS - 1;i ++)								 
    				currentBoard[row][i] = currentBoard[row][i + 1];		
    			currentBoard[row][TOTAL_COLUMNS - 1] = 0;	
    			
    			//Double the cell value since it was merged and add to the current score
    			currentScore += (currentBoard[row][column] <<= 1);					
    		}
    	}
    	//Was the board not changed? If so the move was not possible
    	if(!wasShifted)		return false;
    	moveCount ++;	//At this point in code the move was possible. Spawn a new piece
    	spawn();
    	return true;
    }
    
    public final boolean moveUp () 
    {
    	/*
    	 * Shifts all cells up, combines cells, and calculates the new score. Spawns a new piece if the move was successful
    	 * Returns true if the move was made, false if the move is not possible
    	 */
    	
    	boolean wasShifted = false;	//Determines if any cells been changed. 
    	
    	//Go through each column, shift and combine cells in a upward direction
    	for(int column = 0; column < TOTAL_COLUMNS; column ++)				
    	{
    		int zeroIndex,count;	//zeroIndex is the row index of a zero valued cell. Count is the number of subsequent non-zero cells.
    		
    		//Find the first cell with a zero searching from top to bottom (loop does not contain a body)	
    		for(zeroIndex = TOTAL_ROWS - 1;zeroIndex > 0 && currentBoard[zeroIndex][column] != 0; zeroIndex --);	
    		
    		//Find the next non-zero cell below zeroIndex. Move all subsequent non-zero cells to the location of the non-zero cell
    		for(int nonZeroIndex = zeroIndex - 1; nonZeroIndex >= 0; nonZeroIndex --)
    		{
    			//Does the current cell contain a 0? If so skip it
    			if(currentBoard[nonZeroIndex][column] == 0)		continue;
    			
    			//Has the board NOT been changed yet? If so save the state of the game
    			if(!wasShifted)														
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift all subsequent non-zero cells below [nonZeroIndex][column] to [zeroIndex][column]
    			for(count = 0;nonZeroIndex - count >= 0 && currentBoard[nonZeroIndex - count][column] != 0; count ++) 
    			{
    				currentBoard[zeroIndex - count][column] = currentBoard[nonZeroIndex - count][column];
    				currentBoard[nonZeroIndex - count][column] = 0;
    			}
    			
    			//Range [zeroIndex][column] -> [zeroIndex - count][column] are in proper position. [zeroIndex - count][column] now has a 0
    			nonZeroIndex = (zeroIndex -= count);							
    		}
    		//Combine the same cells next to each other. Once a zero-valued cell is reached all cells below are guaranteed to be a 0
    		for(int row = TOTAL_ROWS - 1; row > 0 && currentBoard[row][column] != 0; row --)					
    		{
    			//Is the current cell not equal to the cell below it? If so skip it
    			if(currentBoard[row][column] != currentBoard[row - 1][column]) continue;
    			
    			//Has the board not been changed yet? If so save the state of the game
    			if(!wasShifted)													
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift cells below [row-1][column] up one position
    			for(int i = row - 1; i > 0;i --)								 
    				currentBoard[i][column] = currentBoard[i - 1][column];		
    			currentBoard[0][column] = 0;							
    			
    			//Double the cell value since it was merged and add to the current score
    			currentScore += (currentBoard[row][column] <<= 1);					
    		}
    	}
    	//Has the board not been changed? If so the move was not possible
    	if(!wasShifted)		return false;
    	moveCount ++;	//At this point in code the move was possible. Spawn a new piece
    	spawn();
    	return true;
    }
    
    public final boolean moveRight () 
    {
    	/*
    	 * Shifts all cells right, combines cells, and calculates the new score. Spawns a new piece if the move was successful
    	 * Returns true if the move was made, false if the move is not possible
    	 */
  
    	boolean wasShifted = false;			//Determines if any cells have been changed
    	
    	//Go through each row, shift and combine cells
    	for(int row = 0; row < TOTAL_ROWS; row ++)										
    	{
    		int zeroIndex, count;	//zeroIndex is the column index of a zero valued cell. Count is the number of subsequent non-zero cells
    		
    		//Find the first cell with a zero in it searching from right to left (loop does not contain a body)	
    		for(zeroIndex = TOTAL_COLUMNS - 1;zeroIndex > 0 && currentBoard[row][zeroIndex] != 0; zeroIndex --);	
    		
    		//Find the next non-zero cell right of zeroIndex. Move all subsequent non-zero cells to the location of the non-zero cell
    		for(int nonZeroIndex = zeroIndex - 1; nonZeroIndex >= 0; nonZeroIndex --)	//Find non-zero cells left of zero and shift right
    		{
    			//Does the current cell contain a 0? If so skip it
    			if(currentBoard[row][nonZeroIndex] == 0)	continue;
    			
    			//Has the board not been changed? Is so then save the state of the game
    			if(!wasShifted)															
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift all subsequent non-zero cells to the left of [row][nonZeroIndex] to [row][zeroIndex]
    			for(count = 0;nonZeroIndex - count >= 0 && currentBoard[row][nonZeroIndex - count] != 0; count ++) 
    			{
    				currentBoard[row][zeroIndex - count] = currentBoard[row][nonZeroIndex - count];
    				currentBoard[row][nonZeroIndex - count] = 0;
    			}
    			
    			//Range [row][zeroIndex] -> [row][zeroIndex - count] are in proper position. [row][zeroIndex - count] now contains a 0
    			nonZeroIndex = (zeroIndex -= count);							
    		}
    		//Combine the same cells next to each other. Once a zero-valued cell is reached all cells left of it are guaranteed to be a 0
    		for(int column = TOTAL_COLUMNS - 1; column > 0 && currentBoard[row][column] != 0; column --)
    		{
    			//Is the current cell not equal to the cell to the left of it? If so skip it 
    			if(currentBoard[row][column] != currentBoard[row][column -1]) 	continue;
    			
    			//Has the board not been changed? Is so then save the state of the game
    			if(!wasShifted)													
    			{
    				saveCurrentState();
    				wasShifted = true;
    			}
    			
    			//Shift cells left of [row-1][column] right one position
    			for(int i = column - 1; i > 0;i --)								 
    				currentBoard[row][i] = currentBoard[row][i-1];			
    			currentBoard[row][0] = 0;	
    			
    			//Double the cell value and add to score
    			currentScore += (currentBoard[row][column] <<= 1);					
    		}
    	}
    	//Has the board not been changed? If so then the move was not possible
    	if(!wasShifted)		return false;
    	moveCount ++;	//At this point in code the move was possible. Spawn a new piece
    	spawn();
    	return true;
    }
    
    public final boolean undo ()
	{
		/*
		 * Reverts back to the last move previous game state (moveCount, the current board, current score, 
		 * and current status are restored)
		 * Returns true if the undo was possible, false if the undo is not possible. 
		 * 
		 * Note: If for some strange reason the user won on the last move and they decide to undo, the game will revert
		 * to playable but will also remove the indication that they have previously won until they make a winning move again.
		 * In this scenario before the undo is executed current currentStatus == GameStatus.WON_BUT_STILL_PLAYABLE or 
		 * currentStatus == GameStatus.WON_BUT_UNPLAYABLE and previousStatus == GameStatus.PLAYABLE.
		 * After the undo currentStatus == GameStatus.PLAYABLE and the previousStatus is invalid
		 */
    	
    	//if there are no saved states return false
    	if(this.savedStates.size() == 0)	return false;
    	
    	//remove the saved state from the top of the stack
    	GameState savedState = this.savedStates.pop();
    	moveCount --;												//Undo is available at this point so revert the move count
		for(int row = 0; row < TOTAL_ROWS; row++)					//Copy the previous board to the current board
			System.arraycopy(savedState.board[row],0, currentBoard[row], 0, TOTAL_COLUMNS);
		currentScore = savedState.score;
		currentStatus = savedState.status;
		return true;
	}
    
    public final GameStatus getGameStatus	()
	{
    	/*
    	 * This function returns the current status of the game. Refer to the declaration of GameStatus for possible game states.
    	 * It is recommended this function is called after a move is made
    	 * If the returned status is GameStatus.WIN the caller needs to call this method again if they want to allow the user to keep 
    	 * playing after a win. Once the method is called again the status is changed to WON_BUT_UNPLAYABLE or WON_BUT_STILL_PLAYABLE
    	 * depending on if moves can be made.
    	 */
    	
    	//Check for a winner only if a winner has not been found yet
    	if(currentStatus == GameStatus.PLAYABLE)
    		for(int row = 0; row < TOTAL_ROWS; row ++)
    			for(int column = 0; column < TOTAL_COLUMNS; column ++)
    				if(currentBoard[row][column] == 2048)	return (currentStatus = GameStatus.WIN);
    	
    	//If the last game status was a win then change it to won but still playable. This ensures that a win will not be shown twice
    	if(currentStatus == GameStatus.WIN)		currentStatus = GameStatus.WON_BUT_STILL_PLAYABLE;
    	
    	/*
    	 *If a zero exists anywhere then it is definitely possible for a move to be made. 
    	 *Return the current status since it will not change
    	*/
    	for(int row = 0; row < TOTAL_ROWS; row ++)
    		for(int column = 0; column < TOTAL_COLUMNS; column ++)
    			if(currentBoard[row][column] == 0)								return currentStatus;
		
    	//Check to see if it possible to shift up or down.
    	for(int row = 0; row < TOTAL_ROWS - 1; row ++)
    		for(int column = 0; column < TOTAL_COLUMNS; column ++)
    			//Are vertically adjacent cells the same? If so a move up or down is possible. The game state does not change
    			if(currentBoard[row][column] == currentBoard[row + 1][column])	return currentStatus;
    	
    	//Check to see if it possible to shift left or right
    	for(int row = 0; row < TOTAL_ROWS ; row ++)
    		for(int column = 0; column < TOTAL_COLUMNS - 1; column ++)
    			//Are horizontally adjacent cells the same? Is so a move left or right is possible. The game state does not change
    			if(currentBoard[row][column] == currentBoard[row][column + 1])	return currentStatus;
    	
    	//At this point no moves are possible, if the game was already won then return that a win has already occurred but no moves exist
    	if(currentStatus == GameStatus.WON_BUT_STILL_PLAYABLE)
    		return (currentStatus = GameStatus.WON_BUT_UNPLAYABLE);
    	return (currentStatus = GameStatus.LOST);		//Since the game has not been won return that the game was lost
	}
    
    public final int getMoveCount()
    {	//Getter for the current move count
    	return this.moveCount;
    }
   
    public final int getScore()
    {	//Getter for the current score
    	return this.currentScore;
    }
    
    public final int getCellValue(int row, int column)
    {	//Getter for the cell value
    	return currentBoard[row][column];
    }
    
    public final void serializeToStream(ObjectOutputStream outputStream) throws IOException
    {
    	/*
    	 * Writes the state of this object to the specified stream
    	 */
    	
    	//Write the board and saved states
    	outputStream.writeObject(currentBoard);
    	outputStream.writeInt(savedStates.size());
    	for(int i = 0; i < savedStates.size(); i ++)
    		savedStates.elementAt(i).serializeToStream(outputStream);
    	
    	//Write the move count, current score, and status
    	outputStream.writeInt(moveCount);
    	outputStream.writeInt(currentScore);
    	outputStream.writeObject(this.currentStatus);
    }
   
	/*
	 * Game board Dimensions: totalRows x totalColumns. [0][0] is bottom left visually
	 * A cell value of 0 represents an empty cell.
	 */
	private int 	  	currentBoard[][];		
	
	/*
	 * A stack of 11 GameState. This is a first in last out stack. 
	 */
	private Stack<GameState> savedStates 	= new Stack<GameState>();    
	private int 		moveCount = 0;							
	private int 		currentScore = 0;
	private GameStatus 	currentStatus = GameStatus.PLAYABLE;	//The current status of the game. 
	private ArrayList<Integer> openCells;						//A list of cells that are able to spawn a number on a new move
	
	class GameState
	{
		//Class holds a saved board and score and the status of the game
		int board[][];
		int score;
		GameStatus status;
		
		GameState(ObjectInputStream inputStream) throws IOException, ClassNotFoundException
		{//Read the class state from the input stream
			board = (int[][])inputStream.readObject();
			score = inputStream.readInt();
			status = (GameStatus)inputStream.readObject();
		}
		GameState()
		{	//Initializes the class by creating a copy of the currentBoard, currentScore and currentStatus
			this.board = new int[TOTAL_ROWS][TOTAL_COLUMNS];
			for(int row = 0; row < TOTAL_ROWS; row++)		//Copy the current board to the previous board (save the board)
				System.arraycopy(currentBoard[row], 0, board[row], 0, TOTAL_COLUMNS);
			score = currentScore;
			status = currentStatus;
		}
		void serializeToStream(ObjectOutputStream outputStream) throws IOException
		{	//Writes the state of the object to the output stream
			outputStream.writeObject(board);
			outputStream.writeInt(score);
			outputStream.writeObject(status);
		}
		
		
	}
	
	private void saveCurrentState()
	{
		/*
		 * Saves the state of the game when a move is made. Saves the status, score, and board
		 */
		GameState saveState = new  GameState();	//Create a save state
		this.savedStates.push(saveState);		//Push the saved state onto the top of the stack
		
		//If ten saved states already exist then remove the bottom saved state (oldest)
		if(this.savedStates.size() > 10)	this.savedStates.remove(0);
	}
	
	private final void spawn()
	{
		/*
		 * Populates a list of open cells. Creates a random 2 or 4, finds an empty cell and places the random 2 or 4 into the empty cell
		 */
		
		/*
		 * Reset the list of open cells and add every open cell to the list. The value added to the list is encoded so only 
    	 * an integer is needed to represent a column and row. The value is encoded as follows
    	 * encodedLocation = row * MAX_BOARD_DIMENSIONS + column;
    	 * row = encodedLocation / MAX_BOARD_DIMENSIONS; column = encodedLocation % MAX_BOARD_DIMENSIONS
    	*/
		openCells.clear();
		for(int row = 0; row < TOTAL_ROWS; row ++)
			for(int column = 0; column < TOTAL_COLUMNS; column ++)
				if(currentBoard[row][column] == 0)	openCells.add(row * MAX_BOARD_DIMENSION + column);	//Is the cell 0? If so encode and add to the list
		
		//Create the random 2 or 4, find an empty cell and set the value of empty cell to the random generator 2 or 4
		int spawnedNumber = (Math.random()  < 0.9) ? 2 : 4;					//Spawned number is a 2, 90% of the time otherwise it is a 4  
		int spawnOpenCellIndex = (int)(Math.random() * openCells.size());	//Get a random index in the range of [0,# open cells) 
		int encodedLocation = openCells.get(spawnOpenCellIndex);			//Get the encoded location and decode it below
		int spawnRow = encodedLocation / MAX_BOARD_DIMENSION, spawnColumn = encodedLocation % MAX_BOARD_DIMENSION; 
		currentBoard[spawnRow][spawnColumn] = spawnedNumber;				//Spawn the random number to a random location
	}
}
