import java.util.Queue

import org.opendaylight.protocol.pcep.PCEPMessage
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject
import org.opendaylight.protocol.pcep.message.PCEPReplyMessage
import org.opendaylight.protocol.pcep.object.CompositeResponseObject
import org.opendaylight.protocol.pcep.tool.MessageGeneratorService

class GroovyReplyMessageGenerator implements MessageGeneratorService {
	
	public GroovyReplyMessageGenerator() {
		}
	
	@Override
	public Queue<PCEPMessage> generateMessages() {
		def queue = new LinkedList<PCEPMessage>()
		queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 7 as Short, 6565 as Long, true, false)
					)
				]
			)
		)

		queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 5 as Short, 235568 as Long, true, false)
					)
				]
			)
		)
		
				queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 7 as Short, 6565 as Long, true, false)
					)
				]
			)
		)

		queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 5 as Short, 235568 as Long, true, false)
					)
				]
			)
		)
		
				queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 7 as Short, 6565 as Long, true, false)
					)
				]
			)
		)

		queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 5 as Short, 235568 as Long, true, false)
					)
				]
			)
		)
		
				queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 7 as Short, 6565 as Long, true, false)
					)
				]
			)
		)

		queue.push(
			new PCEPReplyMessage(
				[
					new CompositeResponseObject(
						new PCEPRequestParameterObject(true, false, true, false, true, 5 as Short, 235568 as Long, true, false)
					)
				]
			)
		)
				
		return queue
	}
}