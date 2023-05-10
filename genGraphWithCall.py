import json

from tqdm import tqdm

from utils.utils import *

entities = {
    "method": set(),
    "package": set(),
    "class": set(),
    "word": set()
}
relations = {
    "c_p": set(),
    "m_c": set(),
    "m_type_word": set(),
    "m_param_type_word": set(),
    "m_param_name_word": set(),
    "m_name_word": set(),
}

def extract(data: dict):
    for pkg, value in data.items():
        entities['package'].add(pkg)
        for item in value:
            try:
                items = item.split(";")
                entities['method'].add(items[0])
                entities['class'].add(items[-2])
                relations["m_c"].add((items[0], items[-2]))
                relations["c_p"].add((items[-2], pkg))
                return_word = items[-3]
                for rw in extractT(return_word):
                    entities['word'].add(rw) # return type
                    relations["m_type_word"].add((items[0], rw))
                method_name = items[0]
                method_name = method_name[:method_name.index("(")]
                method_name = method_name.split(".")[-1]

                words = get_words(method_name)
                for word in words:
                    entities['word'].add(word)
                    relations["m_name_word"].add((items[0], word))

                paramtype = []
                paramname = []

                for param in items[1].split("|"):
                    param = delectT(param).split(" ")
                    if len(param) >= 2:
                        paramtype.append(param[-2])
                        paramname.append(param[-1])
                        entities['word'].add(param[-2])
                        entities['word'].add(param[-1]) # name
                        relations['m_param_type_word'].add((items[0], param[-2]))
                        relations['m_param_name_word'].add((items[0], param[-1]))
            except:
                traceback.print_exc()
                print(item)
                exit(9)
    return entities



def main():
    # 加载配置
    with open("config.json", "r") as f:
        config = json.loads(f.read())
    version = config["android-version"]
    entities = extract(read_jar_json(version))
    entity = {}
    print("解析结果: ")
    for key, value in entities.items():
        entity[key] = list(value)
        print(key, len(entity[key]))
    relation = {}
    for key, value in relations.items():
        relation[key] = list(value)
        print(key, len(relation[key]))
    save_json(version, entity, "entities")
    # save_json(version, relation, "NonCall-entities")
    #以上输出: eneity, relation
    #添加调用关系
    relation['m_c_m'] = set()
    for key, value in tqdm(read_sdk_json(version).items()):
        for call in value['calls']:
            call = call.replace(".framework", "")
            simpleCall = getSimpleMethodName(call)
            # Api 之间的调用
            for api in entity['method']:
                if simpleCall in api:
                    relation['m_c_m'].add((key, api))
    relation['m_c_m'] = list(relation['m_c_m'])
    save_json(version, relation, "relations")



if __name__ == '__main__':
    main()